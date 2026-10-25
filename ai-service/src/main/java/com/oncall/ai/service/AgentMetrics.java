package com.oncall.ai.service;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
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
 *
 * 2026-10-25（P2-2）：双写 Micrometer——原 JSON /metrics 保持不变（人工排查），
 * 同时把同一份数据以 Counter/Timer 形式写入 Micrometer 全局 Registry，
 * 由 actuator 的 /actuator/prometheus 以 Prometheus 文本格式暴露。
 * 键均为低基数（tool/status/reason 固定枚举），无高基数标签风险。
 */
@Component
public class AgentMetrics {

    /** key = "tool:status"，如 "search:success" / "analyze_log:fallback" */
    private final Map<String, AtomicLong> callCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> latencySumMs = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> retrySum = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> tokenUsage = new ConcurrentHashMap<>();
    /** LLM 上游重试计数，key = 触发原因："429" / "5xx" / "connect_error" */
    private final Map<String, AtomicLong> llmRetries = new ConcurrentHashMap<>();

    public void recordTool(String tool, String status, int retries, long latencyMs) {
        callCounts.computeIfAbsent(tool + ":" + status, k -> new AtomicLong()).incrementAndGet();
        latencySumMs.computeIfAbsent(tool, k -> new AtomicLong()).addAndGet(latencyMs);
        retrySum.computeIfAbsent(tool, k -> new AtomicLong()).addAndGet(retries);
        if (isErrorStatus(status)) {
            errorCounts.computeIfAbsent(tool, k -> new AtomicLong()).incrementAndGet();
        }
        // Micrometer 双写（Counter/Timer 按同名同标签自动去重，等价于 computeIfAbsent）
        Metrics.counter("oncall.tool.calls", "tool", tool, "status", status).increment();
        Metrics.counter("oncall.tool.retries", "tool", tool).increment(retries);
        if (latencyMs > 0) {
            Timer.builder("oncall.tool.latency").tag("tool", tool)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(Metrics.globalRegistry)
                    .record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    public void recordTokens(String source, long tokens) {
        tokenUsage.computeIfAbsent(source, k -> new AtomicLong()).addAndGet(tokens);
        Metrics.counter("oncall.tokens", "source", source).increment(tokens);
    }

    /** 记录一次 LLM 上游重试（429 / 5xx / connect_error），供 /metrics 观察限流与上游异常 */
    public void recordLlmRetry(String reason) {
        llmRetries.computeIfAbsent(reason, k -> new AtomicLong()).incrementAndGet();
        Metrics.counter("oncall.llm.retries", "reason", reason).increment();
    }

    /** 记录一次 LLM 上游调用端到端耗时（仅成功尝试），用于区分"模型等待"与"本地处理" */
    public void recordLlmLatency(long latencyMs) {
        if (latencyMs > 0) {
            Timer.builder("oncall.llm.call.latency")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(Metrics.globalRegistry)
                    .record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
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
        snapshot.put("llm_retries", llmRetries.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));
        return snapshot;
    }
}
