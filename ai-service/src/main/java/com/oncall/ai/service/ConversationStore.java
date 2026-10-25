package com.oncall.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oncall.ai.model.Conversation;
import com.oncall.ai.model.Message;
import com.oncall.ai.model.MessageType;
import com.oncall.ai.model.Role;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话上下文存储（内存 + SQLite 写透持久化）
 *
 * 读取路径：纯内存（ConcurrentHashMap），行为与旧内存版完全一致；
 * 写入路径：写透 SQLite（save / markProcessed / remove 时同步落库）；
 * 启动时：从 SQLite 全量加载，进程重启后会话历史、摘要、已处理消息标识可恢复。
 *
 * 语义边界（2026-10-25，P2-1）：
 *   本实现只承诺「持久化」——重启后数据还在。
 *   不承诺「幂等」（外部工具防重复执行需单独设计），
 *   更不是「Checkpoint 恢复」（无执行级状态）。
 *
 * 故障语义：SQLite 任何读写失败都降级为纯内存行为（ERROR 日志 + 继续服务），
 * 不让存储故障打断对话主链路——持久化是增强，不是依赖。
 *
 * 后续可替换为 Redis 或数据库连接池实现，接口不变。
 */
@Component
public class ConversationStore {

    private static final Logger log = LoggerFactory.getLogger(ConversationStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConcurrentHashMap<String, Conversation> store = new ConcurrentHashMap<>();
    // Deduplication: track processed messageIds per conversation
    private final ConcurrentHashMap<String, Set<String>> processedMessages = new ConcurrentHashMap<>();

    private final Object dbLock = new Object();
    private Connection db;  // null = 持久化不可用（降级为纯内存）

    public ConversationStore(@Value("${oncall.db-path:./data/aioncall.db}") String dbPath) {
        try {
            java.io.File f = new java.io.File(dbPath);
            if (f.getParentFile() != null) {
                f.getParentFile().mkdirs();
            }
            db = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // WAL：读写并发友好；单连接 + dbLock 串行写，规避 SQLite 单写者约束
            try (Statement st = db.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS conversations (
                            conv_id     TEXT PRIMARY KEY,
                            created_at  TEXT NOT NULL,
                            updated_at  TEXT NOT NULL,
                            summary     TEXT NOT NULL DEFAULT '',
                            last_intent TEXT NOT NULL DEFAULT '',
                            tools_json  TEXT NOT NULL DEFAULT '[]',
                            sources_json TEXT NOT NULL DEFAULT '[]'
                        )""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            conv_id  TEXT NOT NULL,
                            seq      INTEGER NOT NULL,
                            role     TEXT NOT NULL,
                            content  TEXT NOT NULL,
                            ts       TEXT NOT NULL,
                            type     TEXT NOT NULL,
                            PRIMARY KEY (conv_id, seq)
                        )""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS processed_messages (
                            conv_id TEXT NOT NULL,
                            msg_id  TEXT NOT NULL,
                            PRIMARY KEY (conv_id, msg_id)
                        )""");
            }
            loadFromDb();
            log.info("ConversationStore: SQLite 持久化已启用 path={}, 恢复会话 {} 个, 已处理消息 {} 条",
                    dbPath, store.size(), processedMessages.values().stream().mapToInt(Set::size).sum());
        } catch (Exception e) {
            db = null;
            log.error("ConversationStore: SQLite 初始化失败，降级为纯内存模式（重启后状态不保留）", e);
        }
    }

    // ==================== 启动恢复 ====================

    private void loadFromDb() throws Exception {
        synchronized (dbLock) {
            try (ResultSet rs = db.createStatement().executeQuery(
                    "SELECT conv_id, created_at, updated_at, summary, last_intent, tools_json, sources_json FROM conversations")) {
                while (rs.next()) {
                    Conversation conv = new Conversation(rs.getString("conv_id"));
                    // created_at 是 final，通过消息重放时间轴即可；summary/working memory 直接恢复
                    conv.setSummary(rs.getString("summary"));
                    conv.setLastIntent(rs.getString("last_intent"));
                    for (String t : parseList(rs.getString("tools_json"))) conv.addTool(t);
                    for (String s : parseList(rs.getString("sources_json"))) conv.addSource(s);
                    store.put(conv.conversationId(), conv);
                }
            }
            try (ResultSet rs = db.createStatement().executeQuery(
                    "SELECT conv_id, seq, role, content, ts, type FROM messages ORDER BY conv_id, seq")) {
                while (rs.next()) {
                    Conversation conv = store.get(rs.getString("conv_id"));
                    if (conv == null) continue;  // 孤儿消息（会话被 remove 的竞态），跳过
                    conv.getMessages().add(new Message(
                            Role.valueOf(rs.getString("role")),
                            rs.getString("content"),
                            Instant.parse(rs.getString("ts")),
                            MessageType.valueOf(rs.getString("type"))));
                }
            }
            try (ResultSet rs = db.createStatement().executeQuery("SELECT conv_id, msg_id FROM processed_messages")) {
                while (rs.next()) {
                    processedMessages.computeIfAbsent(rs.getString("conv_id"),
                            k -> ConcurrentHashMap.newKeySet()).add(rs.getString("msg_id"));
                }
            }
        }
    }

    private List<String> parseList(String json) {
        try {
            return json == null || json.isBlank() ? List.of() : MAPPER.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    // ==================== 会话 CRUD（内存为主读路径，写透 DB） ====================

    /** 获取或创建会话 */
    public Conversation getOrCreate(String conversationId) {
        String id = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
        return store.computeIfAbsent(id, Conversation::new);
    }

    /** 查找已有会话 */
    public Optional<Conversation> find(String conversationId) {
        return Optional.ofNullable(store.get(conversationId));
    }

    /** 保存会话（内存 + 写透 SQLite） */
    public void save(Conversation conversation) {
        store.put(conversation.conversationId(), conversation);
        log.debug("Conversation saved: id={}, messages={}",
                conversation.conversationId(), conversation.messages().size());
        persistConversation(conversation);
    }

    /** 清除指定会话 */
    public void remove(String conversationId) {
        store.remove(conversationId);
        processedMessages.remove(conversationId);
        log.debug("Conversation removed: id={}", conversationId);
        if (db == null) return;
        try {
            synchronized (dbLock) {
                try (PreparedStatement ps1 = db.prepareStatement("DELETE FROM conversations WHERE conv_id=?")) {
                    ps1.setString(1, conversationId);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = db.prepareStatement("DELETE FROM messages WHERE conv_id=?")) {
                    ps2.setString(1, conversationId);
                    ps2.executeUpdate();
                }
                try (PreparedStatement ps3 = db.prepareStatement("DELETE FROM processed_messages WHERE conv_id=?")) {
                    ps3.setString(1, conversationId);
                    ps3.executeUpdate();
                }
            }
        } catch (Exception e) {
            log.error("ConversationStore: 会话删除落库失败（内存已删除，DB 留存脏行）id={}", conversationId, e);
        }
    }

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
        if (db == null) return;
        try {
            synchronized (dbLock) {
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT OR IGNORE INTO processed_messages(conv_id, msg_id) VALUES(?,?)")) {
                    ps.setString(1, conversationId);
                    ps.setString(2, messageId);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            log.error("ConversationStore: 已处理消息标识落库失败（重启后该消息可能被重复处理）convId={}, msgId={}",
                    conversationId, messageId, e);
        }
    }

    /** 当前活跃会话数 */
    public int activeCount() {
        return store.size();
    }

    // ==================== 持久化实现 ====================

    /** 会话整体落库：upsert 元数据 + 全量重写消息（会话消息量小，重写比增量同步简单可靠） */
    private void persistConversation(Conversation conv) {
        if (db == null) return;
        try {
            synchronized (dbLock) {
                db.setAutoCommit(false);
                try {
                    upsertConversationMeta(conv);
                    try (PreparedStatement del = db.prepareStatement("DELETE FROM messages WHERE conv_id=?")) {
                        del.setString(1, conv.conversationId());
                        del.executeUpdate();
                    }
                    try (PreparedStatement ins = db.prepareStatement(
                            "INSERT OR REPLACE INTO messages(conv_id, seq, role, content, ts, type) VALUES(?,?,?,?,?,?)")) {
                        int seq = 0;
                        for (Message m : conv.getMessages()) {
                            bindMessage(ins, conv.conversationId(), ++seq, m);
                            ins.addBatch();  // 漏了 addBatch 的 executeBatch 是静默空操作（2026-10-25 实测踩坑）
                        }
                        ins.executeBatch();
                    }
                    db.commit();
                } catch (Exception e) {
                    db.rollback();
                    throw e;
                } finally {
                    db.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            log.error("ConversationStore: 会话落库失败（内存状态不受影响，重启后该会话可能是旧版本）id={}",
                    conv.conversationId(), e);
        }
    }

    private void upsertConversationMeta(Conversation conv) throws Exception {
        try (PreparedStatement ps = db.prepareStatement("""
                INSERT INTO conversations(conv_id, created_at, updated_at, summary, last_intent, tools_json, sources_json)
                VALUES(?,?,?,?,?,?,?)
                ON CONFLICT(conv_id) DO UPDATE SET
                    updated_at=excluded.updated_at, summary=excluded.summary,
                    last_intent=excluded.last_intent, tools_json=excluded.tools_json,
                    sources_json=excluded.sources_json""")) {
            ps.setString(1, conv.conversationId());
            ps.setString(2, conv.getCreatedAt().toString());
            ps.setString(3, conv.getUpdatedAt().toString());
            ps.setString(4, conv.getSummary());
            ps.setString(5, conv.getLastIntent());
            ps.setString(6, MAPPER.writeValueAsString(conv.getRecentTools()));
            ps.setString(7, MAPPER.writeValueAsString(conv.getRecentSources()));
            ps.executeUpdate();
        }
    }

    private void bindMessage(PreparedStatement ins, String convId, int seq, Message m) throws Exception {
        ins.setString(1, convId);
        ins.setInt(2, seq);
        ins.setString(3, m.role().name());
        ins.setString(4, m.content());
        ins.setString(5, m.timestamp().toString());
        ins.setString(6, m.type().name());
    }

    @PreDestroy
    public void close() {
        if (db != null) {
            try {
                synchronized (dbLock) {
                    db.close();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
