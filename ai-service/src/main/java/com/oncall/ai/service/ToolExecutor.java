package com.oncall.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 工具执行器（Tool Runtime 治理层）
 *
 * 原接口 ToolExecutor 已重命名为 {@link Tool}，本类接管"ToolExecutor"这一名字，
 * 作为 Agent 与具体 Tool 之间的统一治理入口。业务工具只实现 {@link Tool#execute}，
 * 全部治理策略由本类统一承担：
 *
 * <ol>
 *   <li>参数校验：执行前校验必填参数</li>
 *   <li>限流：滑动时间窗口（Sliding Window Log，每分钟），防止工具被高频调用</li>
 *   <li>超时控制：CompletableFuture 超时兜底</li>
 *   <li>重试：失败后按 ToolSpec.maxRetries 重试</li>
 *   <li>降级：重试耗尽后返回统一失败结果</li>
 * </ol>
 *
 * Agent 不直接 tool.execute()，而是 toolExecutor.execute(toolName, context)，
 * 这样"工具执行与业务逻辑解耦"才真正成立。所有结构化日志携带 trace_id（MDC）。
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry registry;
    private final AgentMetrics metrics;
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public ToolExecutor(ToolRegistry registry, AgentMetrics metrics) {
        this.registry = registry;
        this.metrics = metrics;
    }

    /**
     * 统一执行入口
     *
     * @param toolId  工具 id（与 ToolSpec.name 一致）
     * @param context 工具上下文（query / conversationId / documentStore）
     */
    public ToolResult execute(String toolId, ToolContext context) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : "n/a";

        Tool tool = registry.get(toolId).orElse(null);
        if (tool == null) {
            log.warn("[traceId={}] 工具不存在: {}", traceId, toolId);
            metrics.recordTool(toolId, "not_found", 0, 0);
            return failureResult(toolId, "工具不存在: " + toolId);
        }
        ToolSpec spec = tool.spec();

        // 1. 参数校验
        List<String> missing = spec.getRequiredParams().stream()
                .filter(p -> isMissing(context, p))
                .toList();
        if (!missing.isEmpty()) {
            log.warn("[traceId={}] 工具={} 参数校验失败，缺少: {}", traceId, toolId, missing);
            metrics.recordTool(toolId, "bad_request", 0, 0);
            return failureResult(toolId, "缺少必填参数: " + missing);
        }

        // 2. 限流（固定窗口 / 每分钟）
        if (spec.getRateLimitPerMin() > 0 && !tryAcquire(toolId, spec.getRateLimitPerMin())) {
            log.warn("[traceId={}] 工具={} 触发限流（>{}/min）", traceId, toolId, spec.getRateLimitPerMin());
            metrics.recordTool(toolId, "rate_limited", 0, 0);
            return failureResult(toolId, "调用过于频繁，已触发限流");
        }

        // 3. 超时 + 重试 + 降级
        int maxAttempt = spec.getMaxRetries() + 1;
        ToolResult result = null;
        for (int attempt = 1; attempt <= maxAttempt; attempt++) {
            try {
                result = executeWithTimeout(tool, context, spec.getTimeoutMs());
                if (isSuccess(result)) {
                    long elapsed = System.currentTimeMillis() - start;
                    log.info("[traceId={}] 工具执行成功 tool={} attempt={}/{} latency={}ms",
                            traceId, toolId, attempt, maxAttempt, elapsed);
                    metrics.recordTool(toolId, "success", attempt - 1, elapsed);
                    return result;
                }
                // 超时为终态：直接返回精确原因，不重试（重试挂死的工具只会浪费时间预算）
                if ("timeout".equals(result.getStatus())) {
                    long elapsed = System.currentTimeMillis() - start;
                    log.warn("[traceId={}] 工具超时 tool={} latency={}ms（终态，不重试）",
                            traceId, toolId, elapsed);
                    metrics.recordTool(toolId, "timeout", attempt - 1, elapsed);
                    return result;
                }
                log.warn("[traceId={}] 工具执行失败 tool={} attempt={}/{} status={}",
                        traceId, toolId, attempt, maxAttempt, result.getStatus());
            } catch (Exception e) {
                log.warn("[traceId={}] 工具执行异常 tool={} attempt={}/{} error={}",
                        traceId, toolId, attempt, maxAttempt, e.getMessage());
            }
        }

        // 重试耗尽 → 降级
        long elapsed = System.currentTimeMillis() - start;
        log.warn("[traceId={}] 工具降级 tool={} 重试耗尽={} latency={}ms",
                traceId, toolId, spec.getMaxRetries(), elapsed);
        metrics.recordTool(toolId, "fallback", spec.getMaxRetries(), elapsed);
        return failureResult(toolId, "工具执行失败（已重试 " + spec.getMaxRetries() + " 次）");
    }

    private boolean isMissing(ToolContext context, String param) {
        return switch (param) {
            case "query" -> context.getQuery() == null || context.getQuery().isBlank();
            case "conversationId" -> context.getConversationId() == null || context.getConversationId().isBlank();
            default -> context.getDocumentStore() == null;
        };
    }

    private boolean isSuccess(ToolResult r) {
        if (r == null) return false;
        String s = r.getStatus();
        return s != null && !"error".equals(s) && !"failed".equals(s);
    }

    private ToolResult executeWithTimeout(Tool tool, ToolContext context, long timeoutMs) {
        if (timeoutMs <= 0) {
            return tool.execute(context);
        }
        // MDC 是 ThreadLocal，异步线程不自动继承；
        // 捕获当前上下文带入工具执行线程，保证 trace_id 贯穿工具内部日志
        java.util.Map<String, String> mdc = MDC.getCopyOfContextMap();
        CompletableFuture<ToolResult> future =
                CompletableFuture.supplyAsync(() -> {
                    if (mdc != null) {
                        MDC.setContextMap(mdc);
                    }
                    try {
                        return tool.execute(context);
                    } finally {
                        MDC.clear();
                    }
                });
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            // timeout 是终态：精确原因不能被重试循环的笼统降级文案覆盖
            return new ToolResult(tool.spec().getName(), "timeout",
                    "工具执行超时（>" + timeoutMs + "ms）", List.of(), "", 0.0, 0.0);
        } catch (Exception e) {
            throw new RuntimeException("工具执行异常", e);
        }
    }

    private boolean tryAcquire(String toolId, int limitPerMin) {
        return rateLimiters.computeIfAbsent(toolId, k -> new RateLimiter(limitPerMin)).tryAcquire();
    }

    private ToolResult failureResult(String toolId, String msg) {
        return new ToolResult(toolId, "failed", msg, List.of(), "", 0.0, 0.0);
    }

    /**
     * 滑动时间窗口限流（Sliding Window Log，每分钟）
     *
     * 语义：任意时刻只统计「当前时间往前 60s」内的请求数，过期时间戳滚动移除，
     * 不存在固定窗口那种整段重置带来的边界突刺（10:00:59 与 10:01:00 不会叠加双倍额度）。
     *
     * 取舍：实现简单、零额外依赖、可精确到毫秒；代价是每 key 保留一份时间戳队列，
     * 且只能"卡上限"、不能平滑速率——若要平滑可替换为令牌桶。
     */
    private static final class RateLimiter {
        private final int limit;
        private final Deque<Long> timestamps = new ArrayDeque<>();

        RateLimiter(int limit) {
            this.limit = limit;
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long windowStart = now - 60_000L;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
