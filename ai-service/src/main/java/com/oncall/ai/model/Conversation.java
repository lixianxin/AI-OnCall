package com.oncall.ai.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次完整的多轮对话
 *
 * 线程安全：内部使用 synchronized 保护 messages 列表。
 */
public record Conversation(
        String conversationId,
        List<Message> messages,
        Instant createTime,
        Instant updateTime
) {
    public Conversation(String conversationId) {
        this(conversationId, new ArrayList<>(), Instant.now(), Instant.now());
    }

    /** 追加消息并更新最后活跃时间 */
    public synchronized Conversation addMessage(Message message) {
        messages.add(message);
        return new Conversation(conversationId, messages, createTime, Instant.now());
    }

    /** 获取最后 N 条消息（用于构建 Prompt 上下文） */
    public synchronized List<Message> lastMessages(int count) {
        int size = messages.size();
        if (size <= count) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(size - count, size));
    }
}
