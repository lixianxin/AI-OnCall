package com.oncall.ai.service;

import java.util.List;

/**
 * 工具元数据（治理参数）
 *
 * 不同 Tool 配不同的 timeoutMs / maxRetries / rateLimitPerMin，
 * 这是 ToolExecutor 治理链能够"按工具差异化治理"的基础，
 * 也是面试讲"为什么这么配"时的具体抓手。
 */
public class ToolSpec {

    private final String name;
    private final String description;
    private final long timeoutMs;
    private final int maxRetries;
    private final int rateLimitPerMin;
    private final List<String> requiredParams;
    /** 是否为写操作工具。写操作受负面清单 + 人工确认双重约束（见 ToolRegistry / ToolExecutor） */
    private final boolean writeOperation;

    public ToolSpec(String name, String description,
                    long timeoutMs, int maxRetries, int rateLimitPerMin,
                    List<String> requiredParams) {
        this(name, description, timeoutMs, maxRetries, rateLimitPerMin, requiredParams, false);
    }

    public ToolSpec(String name, String description,
                    long timeoutMs, int maxRetries, int rateLimitPerMin,
                    List<String> requiredParams, boolean writeOperation) {
        this.name = name;
        this.description = description;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
        this.rateLimitPerMin = rateLimitPerMin;
        this.requiredParams = requiredParams == null ? List.of() : requiredParams;
        this.writeOperation = writeOperation;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getTimeoutMs() { return timeoutMs; }
    public int getMaxRetries() { return maxRetries; }
    public int getRateLimitPerMin() { return rateLimitPerMin; }
    public List<String> getRequiredParams() { return requiredParams; }
    public boolean isWriteOperation() { return writeOperation; }

    @Override
    public String toString() {
        return name + "{timeout=" + timeoutMs + "ms, retries=" + maxRetries
                + ", limit=" + rateLimitPerMin + "/min, required=" + requiredParams + "}";
    }
}
