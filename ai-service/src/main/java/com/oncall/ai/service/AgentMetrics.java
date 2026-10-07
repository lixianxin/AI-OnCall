package com.oncall.ai.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 可观测性指标（内存滑动汇总）
 *
 * 记录每个 Tool 的调用数、错误数、累计延迟、累计重试次数，
 * 供 /metrics 端点输出，回答"瓶颈在哪里"这类问题。
 *
 * 这是学生项目与企业项目的分界线：不只是 print 日志，
 * 而是能聚合出"哪个工具慢、哪个工具错得多"。
 */
@Component
public class AgentMetrics {

    /** key = "tool:status"，如 "search:success" / "analyze_log:fallback" */
    private final Map<String, AtomicLong> callCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> latencySumMs = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> retrySum = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> tokenUsage = new ConcurrentHashMap<>();

    public void recordTool(String tool, String status, int retries, long latencyMs) {
        callCounts.computeIfAbsent(tool + ":" + status, k -> new AtomicLong()).incrementAndGet();
        latencySumMs.computeIfAbsent(tool, k -> new AtomicLong()).addAndGet(latencyMs);
        retrySum.computeIfAbsent(tool, k -> new AtomicLong()).addAndGet(retries);
        if (isErrorStatus(status)) {
            errorCounts.computeIfAbsent(tool, k -> new AtomicLong()).incrementAndGet();
        }
    }

    public void recordTokens(String source, long tokens) {
        tokenUsage.computeIfAbsent(source, k -> new AtomicLong()).addAndGet(tokens);
    }

    private boolean isErrorStatus(String status) {
        return "failed".equals(status) || "fallback".equals(status)
                || "timeout".equals(status) || "rate_limited".equals(status)
                || "bad_request".equals(status) || "not_found".equals(status);
    }

    /** 返回结构化快照，供 /metrics 输出 */
    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Map<String, Object> calls = new TreeMap<>();
        callCounts.forEach((k, v) -> calls.put(k, v.get()));
        Map<String, Object> errors = new TreeMap<>();
        errorCounts.forEach((k, v) -> errors.put(k, v.get()));

        Map<String, Object> perTool = new TreeMap<>();
        latencySumMs.forEach((tool, sum) -> {
            long count = callCounts.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(tool + ":"))
                    .mapToLong(e -> e.getValue().get())
                    .sum();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("calls", count);
            m.put("errors", errorCounts.getOrDefault(tool, new AtomicLong()).get());
            m.put("total_latency_ms", sum.get());
            m.put("avg_latency_ms", count > 0 ? sum.get() / count : 0);
            m.put("total_retries", retrySum.getOrDefault(tool, new AtomicLong()).get());
            perTool.put(tool, m);
        });

        snapshot.put("calls_by_status", calls);
        snapshot.put("errors_by_tool", errors);
        snapshot.put("per_tool", perTool);
        snapshot.put("token_usage", tokenUsage.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));
        return snapshot;
    }
}
