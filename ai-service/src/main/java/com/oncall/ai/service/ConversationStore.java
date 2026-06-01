package com.oncall.ai.service;

import com.oncall.ai.model.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话上下文存储（内存版本）
 *
 * 线程安全：使用 ConcurrentHashMap。
 * 后续可替换为 Redis 或数据库实现，接口不变。
 */
@Component
public class ConversationStore {

    private static final Logger log = LoggerFactory.getLogger(ConversationStore.class);

    private final ConcurrentHashMap<String, Conversation> store = new ConcurrentHashMap<>();

    /** 获取或创建会话 */
    public Conversation getOrCreate(String conversationId) {
        return store.computeIfAbsent(conversationId, Conversation::new);
    }

    /** 查找已有会话 */
    public Optional<Conversation> find(String conversationId) {
        return Optional.ofNullable(store.get(conversationId));
    }

    /** 保存会话 */
    public void save(Conversation conversation) {
        store.put(conversation.conversationId(), conversation);
        log.debug("Conversation saved: id={}, messages={}",
                conversation.conversationId(), conversation.messages().size());
    }

    /** 清除指定会话 */
    public void remove(String conversationId) {
        store.remove(conversationId);
        log.debug("Conversation removed: id={}", conversationId);
    }

    /** 当前活跃会话数 */
    public int activeCount() {
        return store.size();
    }
}
