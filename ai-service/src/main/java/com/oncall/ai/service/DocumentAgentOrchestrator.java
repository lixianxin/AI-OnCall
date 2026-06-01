package com.oncall.ai.service;

import com.oncall.ai.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DocumentAgentOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentAgentOrchestrator.class);

    private static final String SYSTEM_PROMPT = """
            你是企业内部 AI OnCall 助手。
            请严格依据以下提供的文档内容回答用户问题。
            如果文档中没有相关信息，请直接回答：
            "当前文档中未找到相关信息"
            不要编造答案，不要使用文档外的知识。
            请用中文回答。
            """;

    private final ConversationStore conversationStore;
    private final DocumentStore documentStore;
    private final DeepSeekClient deepSeekClient;

    public DocumentAgentOrchestrator(
            ConversationStore conversationStore,
            DocumentStore documentStore,
            DeepSeekClient deepSeekClient) {
        this.conversationStore = conversationStore;
        this.documentStore = documentStore;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public Flux<ChatEvent> stream(String conversationId, String message) {
        log.info("DocumentAgent stream start: convId={}, message={}", conversationId, message);

        // 1. 获取/创建会话，保存用户消息
        Conversation conv = conversationStore.getOrCreate(conversationId);
        Conversation updatedConv = conv.addMessage(Message.ofUser(message));
        conversationStore.save(updatedConv);

        // 2. 意图识别
        String intent = detectIntent(message);

        // 3. 文档检索
        List<DocumentChunk> chunks = documentStore.search(message, 5);
        log.debug("Document search: query={}, found={}", message, chunks.size());

        // 4. 调用 DeepSeek 流式 API
        AtomicReference<StringBuilder> fullReplyRef = new AtomicReference<>(new StringBuilder());

        Flux<ContentEvent> contentFlux = deepSeekClient
                .stream(SYSTEM_PROMPT, updatedConv, message, chunks)
                .doOnNext(token -> fullReplyRef.get().append(token))
                .map(ContentEvent::new);

        // 5. 按 SSE 协议顺序组装事件流
        Flux<ChatEvent> stream = Flux.concat(
                Flux.just(new IntentEvent(intent)),
                Flux.just(new ToolEvent(ToolEvent.SEARCH)),
                Flux.just(new ToolEvent(ToolEvent.READ)),
                contentFlux,
                Flux.just(new DoneEvent(UUID.randomUUID().toString()))
        );

        return stream.cast(ChatEvent.class).doOnComplete(() -> {
            String fullReply = fullReplyRef.get().toString();
            Conversation finalConv = updatedConv.addMessage(Message.ofAssistant(fullReply));
            conversationStore.save(finalConv);
            log.info("DocumentAgent stream done: convId={}, replyLen={}", conversationId, fullReply.length());
        }).doOnError(e -> {
            log.error("DocumentAgent stream error: convId={}", conversationId, e);
            Conversation finalConv = updatedConv.addMessage(Message.ofAssistant(
                    "抱歉，处理您的请求时发生了错误：" + e.getMessage()));
            conversationStore.save(finalConv);
        });
    }

    /**
     * 简单关键词意图识别（与 MockAgentOrchestrator 规则一致）
     */
    private String detectIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("报错") || lower.contains("错误") || lower.contains("失败")
                || lower.contains("异常") || lower.contains("exception")) {
            return IntentEvent.ERROR_DIAGNOSIS;
        }
        if (lower.contains("代码") || lower.contains("生成") || lower.contains("写一个")
                || lower.contains("compose") || lower.contains("kotlin")) {
            return IntentEvent.CODE_GENERATION;
        }
        if (lower.contains("tab") || lower.contains("协议") || lower.contains("注册")
                || lower.contains("权限") || lower.contains("路由") || lower.contains("配置")) {
            return IntentEvent.PROTOCOL_QA;
        }
        return IntentEvent.GENERAL_CHAT;
    }
}
