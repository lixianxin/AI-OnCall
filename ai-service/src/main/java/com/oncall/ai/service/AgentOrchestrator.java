package com.oncall.ai.service;

import com.oncall.ai.model.ChatEvent;
import reactor.core.publisher.Flux;

/**
 * Agent 编排器接口
 *
 * 职责：接收用户消息，协调 Intent 识别 → Tool 执行 → LLM 推理，输出 SSE 事件流。
 * Controller 只依赖此接口，不感知具体 AI 厂商。
 */
public interface AgentOrchestrator {

    /**
     * 处理用户消息，返回流式 ChatEvent
     *
     * @param conversationId 会话 ID
     * @param message 用户输入
     * @return 流式事件流（Intent → Tool → Content × N → Done）
     */
    Flux<ChatEvent> stream(String conversationId, String message);
}
