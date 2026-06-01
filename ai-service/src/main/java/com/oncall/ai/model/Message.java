package com.oncall.ai.model;

import java.time.Instant;

/**
 * 单条聊天消息
 *
 * 从第一天就支持 type 字段，后续 Agent Tool 调用记录可直接存储。
 */
public record Message(
        Role role,
        String content,
        Instant timestamp,
        MessageType type
) {
    public static Message ofUser(String content) {
        return new Message(Role.USER, content, Instant.now(), MessageType.TEXT);
    }

    public static Message ofAssistant(String content) {
        return new Message(Role.ASSISTANT, content, Instant.now(), MessageType.TEXT);
    }

    public static Message ofTool(String content) {
        return new Message(Role.SYSTEM, content, Instant.now(), MessageType.TOOL);
    }
}
