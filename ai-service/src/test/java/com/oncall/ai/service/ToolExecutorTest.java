package com.oncall.ai.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tool Runtime 治理层单测
 *
 * 覆盖分支：参数校验 / 正常执行 / 超时 / 重试 / 降级 / 限流 / 工具不存在 / trace_id 贯穿
 *
 * 不依赖 Spring 容器与真实模型，纯内存构造 ToolRegistry + AgentMetrics，
 * 保证测试快速、稳定、可离线运行。
 */
class ToolExecutorTest {

    private static final DocumentStore NO_STORE = null; // 治理层不触碰 DocumentStore

    private ToolExecutor toolExecutor;
    private AgentMetrics metrics;

    /** 构造指定治理参数的执行器 */
    private ToolExecutor newExecutor(List<Tool> tools) {
        ToolRegistry registry = new ToolRegistry(tools);
        metrics = new AgentMetrics();
        return new ToolExecutor(registry, metrics);
    }

    /** 按 spec 构造一个伪工具 */
    private Tool fakeTool(ToolSpec spec, java.util.function.Function<ToolContext, ToolResult> body) {
        return new Tool() {
            @Override public ToolSpec spec() { return spec; }
            @Override public ToolResult execute(ToolContext context) { return body.apply(context); }
        };
    }

    private ToolSpec spec(String name, long timeoutMs, int maxRetries, int rateLimitPerMin) {
        return new ToolSpec(name, "测试工具 " + name, timeoutMs, maxRetries, rateLimitPerMin,
                List.of("query"));
    }

    private ToolResult ok(String name, String summary) {
        return new ToolResult(name, "done", summary, List.of(), "detail", 0.9, 0.0);
    }

    private ToolContext ctx(String query) {
        return new ToolContext(query, "conv-test", NO_STORE);
    }

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("参数缺失 → 校验失败，不执行工具")
    void missingParamFailsValidation() {
        AtomicInteger calls = new AtomicInteger();
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("t", 1000, 0, 0), c -> { calls.incrementAndGet(); return ok("t", "x"); })));

        ToolResult result = toolExecutor.execute("t", ctx("  ")); // query 为空白

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getSummary()).contains("缺少必填参数");
        assertThat(calls.get()).isZero(); // 工具根本没被调用
    }

    @Test
    @DisplayName("正常调用 → 成功返回工具结果")
    void normalCallSucceeds() {
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("t", 1000, 0, 0), c -> ok("t", "检索到 3 条文档"))));

        ToolResult result = toolExecutor.execute("t", ctx("协议字段怎么配"));

        assertThat(result.getStatus()).isEqualTo("done");
        assertThat(result.getSummary()).isEqualTo("检索到 3 条文档");
    }

    @Test
    @DisplayName("超时 → 返回超时失败结果")
    void timeoutReturnsFailure() throws Exception {
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("slow", 100, 0, 0), c -> {
                    try {
                        Thread.sleep(1000); // 远超 100ms 预算
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ok("slow", "不该返回");
                })));

        long start = System.currentTimeMillis();
        ToolResult result = toolExecutor.execute("slow", ctx("q"));

        assertThat(result.getStatus()).isEqualTo("timeout");
        assertThat(result.getSummary()).contains("超时");
        assertThat(System.currentTimeMillis() - start)
                .as("应在超时预算附近返回，而不是等工具跑完")
                .isLessThan(900);
    }

    @Test
    @DisplayName("第一次失败第二次成功 → 重试后成功")
    void retryRecoversFromTransientFailure() {
        AtomicInteger calls = new AtomicInteger();
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("t", 1000, 1, 0), c -> {
                    if (calls.incrementAndGet() == 1) {
                        return new ToolResult("t", "error", "瞬时异常", List.of(), "", 0.0);
                    }
                    return ok("t", "第二次成功");
                })));

        ToolResult result = toolExecutor.execute("t", ctx("q"));

        assertThat(result.getStatus()).isEqualTo("done");
        assertThat(result.getSummary()).isEqualTo("第二次成功");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("重试耗尽 → 降级返回统一失败结果")
    void exhaustedRetriesDegradeToFallback() {
        AtomicInteger calls = new AtomicInteger();
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("t", 1000, 2, 0), c -> {
                    calls.incrementAndGet();
                    return new ToolResult("t", "error", "持续异常", List.of(), "", 0.0);
                })));

        ToolResult result = toolExecutor.execute("t", ctx("q"));

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getSummary()).contains("已重试 2 次");
        assertThat(calls.get()).isEqualTo(3); // 1 次原始调用 + 2 次重试
    }

    @Test
    @DisplayName("超过限流阈值 → 拒绝调用")
    void rateLimitRejectsExcessCalls() {
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("t", 1000, 0, 2), c -> ok("t", "x"))));

        assertThat(toolExecutor.execute("t", ctx("q")).getStatus()).isEqualTo("done");
        assertThat(toolExecutor.execute("t", ctx("q")).getStatus()).isEqualTo("done");
        ToolResult third = toolExecutor.execute("t", ctx("q")); // 第 3 次/分钟 → 限流

        assertThat(third.getStatus()).isEqualTo("failed");
        assertThat(third.getSummary()).contains("限流");
    }

    @Test
    @DisplayName("工具不存在 → 明确失败结果")
    void unknownToolFailsClearly() {
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("known", 1000, 0, 0), c -> ok("known", "x"))));

        ToolResult result = toolExecutor.execute("nope", ctx("q"));

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getSummary()).contains("工具不存在");
        assertThat(result.getToolName()).isEqualTo("nope");
    }

    @Test
    @DisplayName("trace_id 贯穿治理日志与工具执行线程")
    void traceIdPropagatesIntoToolThread() {
        final String[] seenInTool = new String[1];
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("t", 1000, 0, 0), c -> {
                    seenInTool[0] = MDC.get("traceId"); // 异步线程内读取
                    return ok("t", "x");
                })));

        MDC.put("traceId", "trace-abc123"); // 模拟 TraceIdFilter 写入
        toolExecutor.execute("t", ctx("q"));

        assertThat(seenInTool[0])
                .as("治理层应把 MDC 上下文传播进异步工具线程")
                .isEqualTo("trace-abc123");
        // 治理层自身日志（成功/失败/降级）运行在调用线程，MDC 同样可见
        assertThat(MDC.get("traceId")).isEqualTo("trace-abc123");
    }

    @Test
    @DisplayName("治理结果写入 AgentMetrics（调用数/降级/延迟）")
    void metricsRecordedPerCall() {
        toolExecutor = newExecutor(List.of(
                fakeTool(spec("bad", 1000, 0, 0), c -> new ToolResult("bad", "error", "挂了", List.of(), "", 0.0))));

        toolExecutor.execute("bad", ctx("q"));

        var snapshot = metrics.snapshot();
        @SuppressWarnings("unchecked")
        var perTool = (java.util.Map<String, Object>) snapshot.get("per_tool");
        assertThat(perTool).containsKey("bad");
        @SuppressWarnings("unchecked")
        var bad = (java.util.Map<String, Object>) perTool.get("bad");
        assertThat(bad.get("errors")).isEqualTo(1L);
        assertThat(bad.get("calls")).isEqualTo(1L);
    }
}
