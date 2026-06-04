package com.oncall.ai.service;

import com.oncall.ai.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class DocumentAgentOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentAgentOrchestrator.class);
    private static final int TOP_K = 5;

    private static final String SYSTEM_PROMPT =
        "\u4f60\u662f\u4f01\u4e1a\u5185\u90e8 AI OnCall \u52a9\u624b\u3002\n" +
        "\u3010\u5f3a\u7ea6\u675f\u89c4\u5219\u3011\n" +
        "1. \u4f18\u5148\u4f9d\u636e\u4ee5\u4e0b\u63d0\u4f9b\u7684\u6587\u6863\u5185\u5bb9\u56de\u7b54\u3002\n" +
        "2. \u56de\u7b54\u4e2d\u81ea\u7136\u5f15\u7528\u6765\u6e90\u6587\u4ef6\u540d\uff08\u5982\u300c\u6839\u636e tab-manifest.md\u300d\uff09\uff0c\u4e0d\u8981\u5355\u72ec\u8f93\u51fa\u68c0\u7d22\u7ed3\u679c\u5757\u3002\n" +
        
        "3. \u5982\u679c\u3010\u77e5\u8bc6\u5e93\u5185\u5bb9\u3011\u4e2d\u5305\u542b\u76f8\u5173\u4fe1\u606f\uff0c\u4f60\u5fc5\u987b\u4f18\u5148\u4f9d\u636e\u77e5\u8bc6\u5e93\u56de\u7b54\u3002\n" +
        "4. \u7981\u6b62\u7f16\u9020\u534f\u8bae\u5b57\u6bb5\u548c API\u3002\n" +
        "5. \u8bf7\u7528\u4e2d\u6587\u56de\u7b54\u3002\n" +
        "6. \u56de\u7b54\u672b\u5c3e\u81ea\u52a8\u9644\u4e0a\u672c\u6b21\u68c0\u7d22\u4fe1\u606f\uff0c\u76f4\u63a5\u4ece\u4e0b\u65b9\u3010\u68c0\u7d22\u6307\u6807\u3011\u4e2d\u590d\u5236\uff0c\u4e0d\u8981\u4fee\u6539\u683c\u5f0f\u3002";

    private final ConversationStore conversationStore;
    private final DocumentStore documentStore;
    private final DeepSeekClient deepSeekClient;
    private final KnowledgeMetrics knowledgeMetrics;

    public DocumentAgentOrchestrator(
            ConversationStore conversationStore,
            DocumentStore documentStore,
            DeepSeekClient deepSeekClient,
            KnowledgeMetrics knowledgeMetrics) {
        this.conversationStore = conversationStore;
        this.documentStore = documentStore;
        this.deepSeekClient = deepSeekClient;
        this.knowledgeMetrics = knowledgeMetrics;
    }

    @Override
    public Flux<ChatEvent> stream(String conversationId, String message) {
        log.info("DocumentAgent stream start: convId={}, message={}", conversationId, message);
        if (message == null || message.isBlank()) {
            return Flux.just(new ErrorEvent("EMPTY_MESSAGE", "\u6d88\u606f\u4e0d\u80fd\u4e3a\u7a7a"));
        }

        // 1. Get/create conversation
        Conversation conv = conversationStore.getOrCreate(conversationId);
        Conversation updatedConv = conv.addMessage(Message.ofUser(message));

        // 2. Intent detection
        String intent = detectIntent(message);
        updatedConv.setLastIntent(intent);

        // 3. Execute tool
        String toolName = intentToTool(intent);
        ToolExecutor executor = ToolExecutorFactory.create(toolName);
        ToolContext toolContext = new ToolContext(message, conversationId, documentStore);

        long startMs = System.currentTimeMillis();
        ToolResult toolResult = executor.execute(toolContext);
        long elapsedMs = System.currentTimeMillis() - startMs;

        // ====== RAG DEBUG LOGGING ======
        if (log.isDebugEnabled() && ("search".equals(toolName) || "analyze_log".equals(toolName))) {
            log.debug("======== RAG_DEBUG ========");
            log.debug("Question: {}", message);
            log.debug("Tool: {}", toolName);
            List<SearchResult> debugResults = documentStore.searchWithScores(message, 3);
            for (int i = 0; i < debugResults.size(); i++) {
                SearchResult sr = debugResults.get(i);
                DocumentChunk chunk = sr.getChunk();
                String snippet = chunk.getContent().replace('\n', ' ').replace('\r', ' ');
                if (snippet.length() > 150) snippet = snippet.substring(0, 150) + "...";
                log.debug("  Top{}: file={} score={}", (i+1), chunk.getSource(), String.format("%.2f", sr.getScore()));
                log.debug("  Snippet: {}", snippet);
            }
            log.debug("Confidence: {}, latency: {}ms", String.format("%.2f", toolResult.getConfidence()), elapsedMs);
            log.debug("============================");
        }

        // Metrics tracking
        // Semantic relevance: check if query keywords actually appear in top documents
        // Skip BM25 for queries with too few meaningful Chinese chars (greetings, short questions)
        String chineseOnly = message.replaceAll("[^\\u4e00-\\u9fff]", "");
        boolean tooShort = chineseOnly.length() <= 3;
        boolean rawHit = !tooShort && toolResult.getConfidence() >= 0.5 && !toolResult.getSources().isEmpty();
        boolean keywordOk = rawHit && hasKeywordOverlap(message, documentStore.searchWithScores(message, 3));
        boolean scoreOk = rawHit && toolResult.getBm25MaxScore() >= 3.0;
        boolean hit = keywordOk || scoreOk;
        if (tooShort) {
            log.debug("Query too short ({} Chinese chars), forcing knowledge miss: '{}'", chineseOnly.length(), message);
        }
        // Always use real BM25 score for metrics; hit flag controls only prompt strategy
        double topScore = toolResult.getBm25MaxScore();
        knowledgeMetrics.recordSearch(hit, elapsedMs, topScore);

        // Build SourcesEvent with real BM25 scores
        List<SourcesEvent.SourceItem> sourceItems;
        if ("search".equals(toolName) || "analyze_log".equals(toolName)) {
            List<SearchResult> searchResults = documentStore.searchWithScores(message, TOP_K);
            sourceItems = searchResults.stream()
                .filter(r -> r.getScore() >= 0.5)
                .map(r -> {
                    DocumentChunk chunk = r.getChunk();
                    String snippet = chunk.getContent();
                    if (snippet.length() > 120) snippet = snippet.substring(0, 120) + "...";
                    String fileName = chunk.getSource().contains(" > ")
                        ? chunk.getSource().split(" > ")[0]
                        : chunk.getSource();
                    return new SourcesEvent.SourceItem(fileName, chunk.getSource(), r.getScore(), snippet);
                })
                .collect(Collectors.toList());
        } else {
            sourceItems = toolResult.getSources().stream()
                .map(src -> new SourcesEvent.SourceItem(src, "", 0.0, ""))
                .collect(Collectors.toList());
        }

        // Update Working Memory
        updatedConv.addTool(toolName);
        for (SourcesEvent.SourceItem si : sourceItems) {
            updatedConv.addSource(si.file());
        }

        log.debug("Tool {} executed: confidence={}, sources={}, latency={}ms",
                toolName, String.format("%.2f", toolResult.getConfidence()),
                toolResult.getSources().size(), elapsedMs);

        // Count relevant docs from already-built sourceItems (same data source, no re-search)
        long relevantDocCount = sourceItems.size();

        // Build enriched prompt with tool results and metrics
        String metricsLine = knowledgeMetrics.formatSearchMetrics(
                (int) relevantDocCount, topScore, elapsedMs);

        // Code-determined hit/miss: tell the model what to do instead of letting it decide
        String ruleBlock;
        if (hit) {
            ruleBlock = "\n\n\u3010\u5173\u952e\u89c4\u5219\u3011\n"
                + "\u4ee5\u4e0a\u77e5\u8bc6\u5e93\u5185\u5bb9\u5df2\u7ecf\u7cfb\u7edf\u68c0\u7d22\u786e\u8ba4\u4e0e\u95ee\u9898\u76f8\u5173\uff0c\u4f60\u5fc5\u987b\u4f9d\u636e\u77e5\u8bc6\u5e93\u5185\u5bb9\u56de\u7b54\uff0c\u5e76\u81ea\u7136\u5f15\u7528\u6765\u6e90\u6587\u4ef6\u540d\u3002\u56de\u7b54\u672b\u5c3e\u53e6\u8d77\u4e00\u884c\u8f93\u51fa\u4e0b\u65b9\u3010\u68c0\u7d22\u6307\u6807\u3011\u4e2d\u7684\u5185\u5bb9\u3002";
        } else {
            ruleBlock = "\n\n\u3010\u5173\u952e\u89c4\u5219\u3011\n"
                + "\u77e5\u8bc6\u5e93\u672a\u547d\u4e2d\u76f8\u5173\u5185\u5bb9\u3002\u4f60\u5e94\u5f53\uff1a\n"
                + "1. \u8f93\u51fa\u3010\u77e5\u8bc6\u5e93\u672a\u547d\u4e2d\u3011\n"
                + "2. \u57fa\u4e8e\u4f60\u7684\u8bad\u7ec3\u77e5\u8bc6\u56de\u7b54\u7528\u6237\u95ee\u9898\n"
                + "3. \u6807\u6ce8\u300c\u4ee5\u4e0b\u56de\u7b54\u6765\u81ea AI \u6a21\u578b\u77e5\u8bc6\u5e93\uff0c\u4ec5\u4f9b\u53c2\u8003\u300d\n"
                + "\u56de\u7b54\u672b\u5c3e\u53e6\u8d77\u4e00\u884c\u8f93\u51fa\u4e0b\u65b9\u3010\u68c0\u7d22\u6307\u6807\u3011\u4e2d\u7684\u5185\u5bb9\u3002";
        }
        // Build Memory context from Conversation (summary + recent tools + recent sources + history)
        String memoryContext = updatedConv.buildMemoryContext();

        String enrichedPrompt = SYSTEM_PROMPT
                + memoryContext
                + "\n\n\u3010\u68c0\u7d22\u6307\u6807\u3011\n" + metricsLine
                + "\n\n\u3010\u77e5\u8bc6\u5e93\u5185\u5bb9\u3011\n" + toolResult.getDetailData()
                + ruleBlock;

        AtomicReference<StringBuilder> fullReplyRef = new AtomicReference<>(new StringBuilder());

        Flux<ContentEvent> contentFlux = deepSeekClient
                .stream(enrichedPrompt, updatedConv, message, toolResult.getDetailData())
                .doOnNext(token -> fullReplyRef.get().append(token))
                .map(ContentEvent::new);

        // 4. SSE event stream: Intent -> Tool(done+summary) -> Sources(with scores) -> Content -> Done
        Flux<ChatEvent> stream = Flux.concat(
                Flux.just(new IntentEvent(intent)),
                Flux.just(new ToolEvent(toolName, "done", toolResult.getSummary())),
                Flux.just(new SourcesEvent(sourceItems)),
                contentFlux,
                Flux.just(new DoneEvent(UUID.randomUUID().toString()))
        );

        return stream.cast(ChatEvent.class).doOnComplete(() -> {
            String fullReply = fullReplyRef.get().toString();
            Conversation finalConv = updatedConv.addMessage(Message.ofAssistant(fullReply));

            // Summary Memory
            if (finalConv.needsSummarization() && finalConv.getSummary().isEmpty()) {
                triggerSummary(finalConv);
            }

            conversationStore.save(finalConv);
            log.info("DocumentAgent stream done: convId={}, replyLen={}",
                    conversationId, fullReply.length());
        }).doOnError(e -> {
            log.error("DocumentAgent stream error: convId={}", conversationId, e);
        });
    }

    private void triggerSummary(Conversation conv) {
        try {
            String historyText = conv.getRecentMessages(20).stream()
                    .map(m -> (m.role() == com.oncall.ai.model.Role.USER ? "\u7528\u6237" : "AI") + ": " + m.content())
                    .collect(Collectors.joining("\n"));
            String summaryPrompt = "\u8bf7\u5bf9\u4ee5\u4e0b\u5bf9\u8bdd\u751f\u6210\u7b80\u6d01\u7684\u4e2d\u6587\u6458\u8981\uff0850\u5b57\u4ee5\u5185\uff09\uff1a\n" + historyText;
            Flux<String> summaryFlux = deepSeekClient.stream(
                    "\u4f60\u662f\u4e00\u4e2a\u5bf9\u8bdd\u6458\u8981\u52a9\u624b\u3002", conv, summaryPrompt, "");
            StringBuilder sb = new StringBuilder();
            summaryFlux.subscribe(
                    sb::append,
                    err -> log.warn("Summary generation failed: {}", err.getMessage()),
                    () -> {
                        conv.setSummary(sb.toString().trim());
                        log.info("Summary generated: {}", conv.getSummary());
                    }
            );
        } catch (Exception e) {
            log.warn("Failed to generate summary: {}", e.getMessage());
        }
    }

    private String intentToTool(String intent) {
        return switch (intent) {
            case IntentEvent.ERROR_DIAGNOSIS -> ToolEvent.ANALYZE_LOG;
            case IntentEvent.CODE_GENERATION -> ToolEvent.GENERATE;
            case IntentEvent.GENERAL_CHAT -> ToolEvent.SEARCH;
            default -> ToolEvent.SEARCH;
        };
    }

    /** Check if query Chinese bigrams appear in top document content. Uses bigram overlap ratio >= 30%. */
    private boolean hasKeywordOverlap(String query, java.util.List<SearchResult> results) {
        if (results.isEmpty()) return false;
        String topContent = results.get(0).getChunk().getContent().toLowerCase();
        // Extract Chinese bigrams from query
        java.util.List<String> bigrams = new java.util.ArrayList<>();
        StringBuilder run = new StringBuilder();
        for (char c : query.toCharArray()) {
            if (c >= 0x4e00 && c <= 0x9fff) {
                run.append(c);
            } else {
                // Extract bigrams from the Chinese run
                for (int i = 0; i <= run.length() - 2; i++) {
                    bigrams.add(run.substring(i, i + 2));
                }
                run.setLength(0);
            }
        }
        for (int i = 0; i <= run.length() - 2; i++) {
            bigrams.add(run.substring(i, i + 2));
        }
        if (bigrams.isEmpty()) return true; // No Chinese text, trust BM25
        // Also add English words (2+ chars) from query
        for (String word : query.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (word.length() >= 2) bigrams.add(word);
        }
        long matched = bigrams.stream().filter(kw -> topContent.contains(kw)).count();
        double ratio = (double) matched / bigrams.size();
        log.debug("Keyword overlap: {}/{} = {} for query={}", matched, bigrams.size(), String.format("%.2f", ratio), query);
        return ratio >= 0.2;
    }

        private String detectIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("\u9519\u8bef") || lower.contains("\u5f02\u5e38") || lower.contains("\u5931\u8d25")
                || lower.contains("exception") || lower.contains("npe")
                || lower.contains("\u8d85\u65f6") || lower.contains("timeout")
                || lower.contains("\u9ed1\u5c4f") || lower.contains("ssl")) {
            return IntentEvent.ERROR_DIAGNOSIS;
        }
        if (lower.contains("\u4ee3\u7801") || lower.contains("\u751f\u6210") || lower.contains("\u5199\u4e00\u4e2a")
                || lower.contains("compose") || lower.contains("kotlin")
                || lower.contains("\u5b9e\u73b0") || lower.contains("repository")) {
            return IntentEvent.CODE_GENERATION;
        }
        if (lower.contains("tab") || lower.contains("\u534f\u8bae") || lower.contains("\u6ce8\u518c")
                || lower.contains("\u6743\u9650") || lower.contains("\u8def\u7531") || lower.contains("\u914d\u7f6e")
                || lower.contains("manifest")) {
            return IntentEvent.PROTOCOL_QA;
        }
        return IntentEvent.GENERAL_CHAT;
    }
}