package com.oncall.ai.model;

/**
 * Android 端发送的聊天请求
 */
public record ChatRequest(
        String message,
        String conversationId,
        String messageId
) {
}
