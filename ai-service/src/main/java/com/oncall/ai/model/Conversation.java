package com.oncall.ai.model;

import java.time.Instant;
import java.util.ArrayList;
import com.oncall.ai.model.Role;
import com.oncall.ai.model.Message;
import java.util.List;

public class Conversation {

    private final String id;
    private final List<Message> messages = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;

    // Layer 2: Summary Memory
    private String summary = "";

    // Layer 3: Agent Working Memory
    private String lastIntent = "";
    private final List<String> recentTools = new ArrayList<>();
    private final List<String> recentSources = new ArrayList<>();

    private static final int MAX_HISTORY = 20;
    private static final int MAX_TOOLS = 10;
    private static final int MAX_SOURCES = 10;
    private static final int SUMMARY_THRESHOLD = 10;

    public Conversation(String id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public List<Message> getMessages() { return messages; }
    public List<Message> messages() { return messages; }
    public Instant getCreatedAt() { return createdAt; }
    public String conversationId() { return id; }
    public Instant getUpdatedAt() { return updatedAt; }

    // === Layer 1: Conversation Memory ===

    public Conversation addMessage(Message message) {
        this.messages.add(message);
        this.updatedAt = Instant.now();
        return this;
    }

    public List<Message> getRecentMessages(int count) {
        int size = messages.size();
        if (size == 0) return List.of();
        count = Math.min(count, size);
        return new ArrayList<>(messages.subList(size - count, size));
    }

    public List<Message> getRecentMessages() {
        return getRecentMessages(MAX_HISTORY);
    }

    public boolean needsSummarization() {
        return messages.size() >= SUMMARY_THRESHOLD;
    }

    // === Layer 2: Summary Memory ===

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getHistoryMessageCount() {
        if (summary.isEmpty()) {
            return messages.size();
        }
        // After summarization, only keep recent messages
        return Math.min(messages.size(), MAX_HISTORY);
    }

    // === Layer 3: Agent Working Memory ===

    public String getLastIntent() { return lastIntent; }
    public void setLastIntent(String lastIntent) { this.lastIntent = lastIntent; }

    public List<String> getRecentTools() { return recentTools; }

    public void addTool(String tool) {
        recentTools.add(tool);
        if (recentTools.size() > MAX_TOOLS) {
            recentTools.remove(0);
        }
    }

    public List<String> getRecentSources() { return recentSources; }

    public void addSource(String source) {
        if (!recentSources.contains(source)) {
            recentSources.add(source);
        }
        if (recentSources.size() > MAX_SOURCES) {
            recentSources.remove(0);
        }
    }

    // === Build context for LLM prompt ===

    public String buildMemoryContext() {
        StringBuilder sb = new StringBuilder();

        // Layer 2: Summary
        if (!summary.isEmpty()) {
            sb.append("【对话摘要】\n").append(summary).append("\n\n");
        }

        // Layer 3: Recent tools
        if (!recentTools.isEmpty()) {
            sb.append("【最近使用工具】\n");
            for (String tool : recentTools.subList(Math.max(0, recentTools.size() - 5), recentTools.size())) {
                sb.append("- ").append(tool).append("\n");
            }
            sb.append("\n");
        }

        // Layer 3: Recent sources
        if (!recentSources.isEmpty()) {
            sb.append("【最近引用文档】\n");
            for (String src : recentSources.subList(Math.max(0, recentSources.size() - 5), recentSources.size())) {
                sb.append("- ").append(src).append("\n");
            }
            sb.append("\n");
        }

        // Layer 1: Recent conversation history (last 5 exchanges)
        List<Message> recent = getRecentMessages(10);
        if (!recent.isEmpty()) {
            sb.append("【最近对话】\n");
            for (Message msg : recent) {
                String role = msg.role() == Role.USER ? "用户" : "AI";
                sb.append(role).append(": ").append(msg.content()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
