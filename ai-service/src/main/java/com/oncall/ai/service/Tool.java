package com.oncall.ai.service;

/**
 * 工具契约接口（原 ToolExecutor 接口重命名而来）
 *
 * 每个 Tool 只关注自身业务逻辑；注册、校验、限流、超时、重试、降级
 * 统一由 {@link ToolExecutor} 治理层承担，实现"工具执行与业务逻辑解耦"。
 */
public interface Tool {

    /** 工具元数据：名称 / 描述 / 超时 / 重试 / 限流 / 必填参数 */
    ToolSpec spec();

    /** 工具业务执行（仅关注业务，治理策略由 ToolExecutor 包裹） */
    ToolResult execute(ToolContext context);
}
