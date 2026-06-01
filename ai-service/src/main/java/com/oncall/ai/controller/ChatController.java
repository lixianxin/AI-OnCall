package com.oncall.ai.controller;

import com.oncall.ai.model.ChatEvent;
import com.oncall.ai.model.ChatRequest;
import com.oncall.ai.model.ErrorEvent;
import com.oncall.ai.service.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * AI OnCall SSE 流式接口
 *
 * 职责边界：
 * - 解析请求 DTO
 * - 调用 AgentOrchestrator（不感知具体 AI 厂商）
 * - 将 ChatEvent 序列化为 SSE ServerSentEvent
 *
 * 不直接依赖 DeepSeek / OpenAI / ConversationStore / ToolExecutor。
 */
@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentOrchestrator orchestrator;

    public ChatController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * 流式聊天接口
     *
     * @param request ChatRequest（message + conversationId）
     * @return SSE 事件流（Intent → Tool → Content → Done）
     */
    @PostMapping(
            value = "/api/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        log.info("SSE request: convId={}, message={}", conversationId, request.message());

        return orchestrator.stream(conversationId, request.message())
                .map(event -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(serializeEvent(event))
                        .build())
                .onErrorResume(error -> {
                    log.error("Stream error: convId={}", conversationId, error);
                    var errorEvent = new ErrorEvent(
                            "STREAM_ERROR",
                            error.getMessage() != null ? error.getMessage() : "Unknown error"
                    );
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("message")
                            .data(serializeEvent(errorEvent))
                            .build());
                })
                .doOnComplete(() -> log.info("SSE completed: convId={}", conversationId))
                .doOnCancel(() -> log.info("SSE cancelled: convId={}", conversationId));
    }

    /**
     * 将 ChatEvent 序列化为 JSON 字符串
     *
     * ChatEvent 使用 @JsonTypeInfo，Jackson 自动注入 type 字段。
     * 输出格式与 Android 端 OnCallStreamEvent 协议一致。
     */
    private String serializeEvent(ChatEvent event) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(event);
        } catch (Exception e) {
            log.error("Serialize error: type={}", event.type(), e);
            return "{\"type\":\"error\",\"code\":\"SERIALIZE_ERROR\",\"delta\":\"序列化失败\"}";
        }
    }
}
