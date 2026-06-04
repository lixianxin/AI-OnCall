import os

# 1. Add dedup to ConversationStore
filepath = r"D:\AI-OnCall\ai-service\src\main\java\com\oncall\ai\service\ConversationStore.java"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Add imports
old_import = "import java.util.Optional;"
new_import = "import java.util.Optional;\nimport java.util.Set;\nimport java.util.concurrent.ConcurrentHashMap;\nimport java.util.Collections;"
content = content.replace(old_import, new_import)

# Add dedup field and method before the closing brace of the class
old_end = """    /** 当前活跃会话数 */
    public int activeCount() {
        return store.size();
    }
}"""

new_end = """    // Deduplication: track processed messageIds per conversation
    private final ConcurrentHashMap<String, Set<String>> processedMessages = new ConcurrentHashMap<>();

    /** Check if this message was already processed for this conversation */
    public boolean isDuplicate(String conversationId, String messageId) {
        if (messageId == null || messageId.isBlank()) return false;
        Set<String> ids = processedMessages.get(conversationId);
        return ids != null && ids.contains(messageId);
    }

    /** Mark a message as processed for deduplication */
    public void markProcessed(String conversationId, String messageId) {
        if (messageId == null || messageId.isBlank()) return;
        processedMessages.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(messageId);
    }

    /** 当前活跃会话数 */
    public int activeCount() {
        return store.size();
    }
}"""

content = content.replace(old_end, new_end)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

# 2. Add dedup check in ChatController
filepath2 = r"D:\AI-OnCall\ai-service\src\main\java\com\oncall\ai\controller\ChatController.java"

with open(filepath2, "r", encoding="utf-8") as f:
    content2 = f.read()

# Add dedup check before streaming
old_check = """        log.info("SSE stream start: convId={}, msgLen={}", conversationId, message.length());"""

new_check = """        // Deduplication: skip if this messageId was already processed (retry guard)
        if (request.messageId() != null && !request.messageId().isBlank()
                && conversationStore.isDuplicate(conversationId, request.messageId())) {
            log.warn("Duplicate message detected: convId={}, msgId={}", conversationId, request.messageId());
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"type\":\"error\",\"code\":\"DUPLICATE_MESSAGE\",\"delta\":\"消息已处理，跳过重复请求\"}")
                    .build());
        }
        conversationStore.markProcessed(conversationId, request.messageId());

        log.info("SSE stream start: convId={}, msgLen={}", conversationId, message.length());"""

content2 = content2.replace(old_check, new_check)

# Need to inject ConversationStore into ChatController
old_field = """    private final AgentOrchestrator agentOrchestrator;
    private final KnowledgeMetrics knowledgeMetrics;"""

new_field = """    private final AgentOrchestrator agentOrchestrator;
    private final KnowledgeMetrics knowledgeMetrics;
    private final com.oncall.ai.service.ConversationStore conversationStore;"""

content2 = content2.replace(old_field, new_field)

old_constructor = """    public ChatController(AgentOrchestrator agentOrchestrator, KnowledgeMetrics knowledgeMetrics) {
        this.agentOrchestrator = agentOrchestrator;
        this.knowledgeMetrics = knowledgeMetrics;
    }"""

new_constructor = """    public ChatController(AgentOrchestrator agentOrchestrator, KnowledgeMetrics knowledgeMetrics,
                          com.oncall.ai.service.ConversationStore conversationStore) {
        this.agentOrchestrator = agentOrchestrator;
        this.knowledgeMetrics = knowledgeMetrics;
        this.conversationStore = conversationStore;
    }"""

content2 = content2.replace(old_constructor, new_constructor)

with open(filepath2, "w", encoding="utf-8") as f:
    f.write(content2)

print("P0-2 done: dedup in ConversationStore + ChatController guard")
