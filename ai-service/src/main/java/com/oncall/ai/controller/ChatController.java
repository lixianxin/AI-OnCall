package com.oncall.ai.controller;

import com.oncall.ai.model.ChatEvent;
import com.oncall.ai.model.ChatRequest;
import com.oncall.ai.model.ErrorEvent;
import com.oncall.ai.service.AgentOrchestrator;
import com.oncall.ai.service.KnowledgeMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long HEARTBEAT_INTERVAL_MS = 15_000;

    private final AgentOrchestrator agentOrchestrator;
    private final KnowledgeMetrics knowledgeMetrics;
    private final com.oncall.ai.service.ConversationStore conversationStore;

    public ChatController(AgentOrchestrator agentOrchestrator, KnowledgeMetrics knowledgeMetrics,
                          com.oncall.ai.service.ConversationStore conversationStore) {
        this.agentOrchestrator = agentOrchestrator;
        this.knowledgeMetrics = knowledgeMetrics;
        this.conversationStore = conversationStore;
    }

    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody String rawBody) {
        // Parse JSON manually to handle record deserialization issues
        ChatRequest request;
        try {
            request = objectMapper.readValue(rawBody, ChatRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse ChatRequest: rawBody={}, error={}", rawBody, e.getMessage());
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"type\":\"error\",\"code\":\"PARSE_ERROR\",\"delta\":\"闁荤姴娲弨閬嶆儑閹殿喗鍠嗛柨婵嗘閳ь剝濮ゅ鍕綇椤愩儛? " + e.getMessage() + "\"}")
                    .build());
        }
        String conversationId = request.conversationId();
        String message = request.message();

        // Deduplication: skip if this messageId was already processed (retry guard)
        if (request.messageId() != null && !request.messageId().isBlank()
                && conversationStore.isDuplicate(conversationId, request.messageId())) {
            log.warn("Duplicate message detected: convId={}, msgId={}", conversationId, request.messageId());
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"type\":\"error\",\"code\":\"DUPLICATE_MESSAGE\",\"delta\":\"Duplicate message skipped\"}")
                    .build());
        }
        conversationStore.markProcessed(conversationId, request.messageId());

        log.info("SSE stream start: convId={}, msgLen={}", conversationId, message.length());

        AtomicLong eventId = new AtomicLong(0);

        // Main event stream with proper SSE event builder
        Flux<ServerSentEvent<String>> mainStream = agentOrchestrator
                .stream(conversationId, message)
                .map(event -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(eventId.incrementAndGet()))
                        .event(event.type())
                        .data(serializeEvent(event))
                        .build())
                .doOnComplete(() -> log.info("SSE stream complete: convId={}", conversationId))
                .doOnError(e -> log.error("SSE stream error: convId={}", conversationId, e));

        // Heartbeat: SSE comment every 15s to keep connection alive
        Flux<ServerSentEvent<String>> heartbeat = Flux
                .interval(Duration.ofMillis(HEARTBEAT_INTERVAL_MS))
                .map(i -> ServerSentEvent.<String>builder()
                        .comment("keepalive")
                        .build())
                .doOnNext(h -> log.trace("SSE heartbeat: convId={}", conversationId));

        // Assemble and handle errors gracefully
        return Flux.merge(mainStream, heartbeat)
                .onErrorResume(e -> {
                    log.error("SSE stream error, sending error event: convId={}", conversationId, e);
                    String errJson = "{\"type\":\"error\",\"code\":\"STREAM_ERROR\",\"delta\":\"\"}";
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errJson)
                            .build());
                })
                .doOnCancel(() -> log.info("SSE stream cancelled: convId={}", conversationId));
    }

    @GetMapping("/api/chat/metrics")
    public Map<String, Object> metrics() {
        return java.util.Map.of(
            "totalSearches", knowledgeMetrics.getTotalSearches(),
            "hitCount", knowledgeMetrics.getHitCount(),
            "missCount", knowledgeMetrics.getMissCount(),
            "hitRate", String.format("%.2f", knowledgeMetrics.getHitRate() * 100) + "%",
            "avgLatencyMs", String.format("%.1f", knowledgeMetrics.getAvgLatencyMs()),
            "maxBm25Score", String.format("%.2f", knowledgeMetrics.getMaxBm25Score())
        );
    }

    private String serializeEvent(ChatEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Serialize error: type={}", event.type(), e);
            return "{\"type\":\"error\",\"code\":\"SERIALIZE_ERROR\",\"delta\":\"\"}";
        }
    }
}
