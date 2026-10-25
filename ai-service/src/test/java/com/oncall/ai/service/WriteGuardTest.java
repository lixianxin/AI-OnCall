package com.oncall.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 写操作负面清单 + 人工确认门测试（2026-10-25 补齐简历声明 #3 时新增）
 *
 * 本机（中文用户路径 + GBK）Gradle test worker 损坏，同语义验证曾以
 * VerifyWriteGuard 独立 classpath 运行通过 4/4；本测试在 CI（ubuntu）执行。
 */
class WriteGuardTest {

    private Tool fakeTool(ToolSpec spec) {
        return new Tool() {
            @Override public ToolSpec spec() { return spec; }
            @Override public ToolResult execute(ToolContext context) {
                return new ToolResult(spec.getName(), "success", "executed", null, "ok", 0.9);
            }
        };
    }

    @Test
    @DisplayName("负面清单：高危写操作工具拒绝注册")
    void denylistToolRegistrationRejected() {
        ToolSpec dangerous = new ToolSpec("restart_service", "高危写操作",
                1000, 0, 0, List.of(), true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ToolRegistry(List.of(fakeTool(dangerous))));
        assertTrue(ex.getMessage().contains("写操作负面清单"));
    }

    @Test
    @DisplayName("人工确认门：写操作未确认返回 confirmation_required 且不执行")
    void unconfirmedWriteBlocked() {
        ToolRegistry registry = new ToolRegistry(List.of(fakeTool(new ToolSpec(
                "clear_cache", "合法写操作", 1000, 0, 0, List.of(), true))));
        ToolExecutor executor = new ToolExecutor(registry, new AgentMetrics());
        ToolResult result = executor.execute("clear_cache",
                new ToolContext("q", "c1", null, false));
        assertEquals("confirmation_required", result.getStatus());
    }

    @Test
    @DisplayName("人工确认门：写操作确认后正常执行")
    void confirmedWriteExecutes() {
        ToolRegistry registry = new ToolRegistry(List.of(fakeTool(new ToolSpec(
                "clear_cache", "合法写操作", 1000, 0, 0, List.of(), true))));
        ToolExecutor executor = new ToolExecutor(registry, new AgentMetrics());
        ToolResult result = executor.execute("clear_cache",
                new ToolContext("q", "c1", null, true));
        assertEquals("success", result.getStatus());
    }

    @Test
    @DisplayName("回归保护：只读工具不受确认门影响")
    void readonlyToolUnaffected() {
        ToolRegistry registry = new ToolRegistry(List.of(fakeTool(new ToolSpec(
                "search", "只读", 1000, 0, 0, List.of(), false))));
        ToolExecutor executor = new ToolExecutor(registry, new AgentMetrics());
        ToolResult result = executor.execute("search",
                new ToolContext("q", "c1", null, false));
        assertEquals("success", result.getStatus());
    }
}
