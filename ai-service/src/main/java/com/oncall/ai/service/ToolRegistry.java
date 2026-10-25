package com.oncall.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表（取代原 ToolExecutorFactory 的 switch 选择）
 *
 * 通过构造器注入 List&lt;Tool&gt;，Spring 自动收集所有 Tool Bean 并注册。
 * 上层（Agent 编排）通过 toolId 查找工具，避免硬编码依赖具体实现类。
 *
 * 幂等校验：重复 id 直接抛异常，启动即暴露配置错误。
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    /**
     * 写操作负面清单：命中名单的写操作工具一律拒绝注册（启动即失败），
     * 从机制上保证高危写操作不可能经由 Agent 工具链路触达生产系统。
     */
    public static final java.util.Set<String> WRITE_TOOL_DENYLIST = java.util.Set.of(
            "restart_service", "stop_service", "deploy", "rollback_deploy",
            "delete_data", "drop_table", "truncate_table", "kill_process", "modify_config");

    public ToolRegistry(List<Tool> toolBeans) {
        for (Tool tool : toolBeans) {
            ToolSpec spec = tool.spec();
            if (tools.containsKey(spec.getName())) {
                throw new IllegalStateException("重复注册的工具 id: " + spec.getName());
            }
            if (spec.isWriteOperation() && WRITE_TOOL_DENYLIST.contains(spec.getName())) {
                throw new IllegalArgumentException(
                        "写操作负面清单拦截: " + spec.getName() + " 属于禁止注册的高危写操作");
            }
            tools.put(spec.getName(), tool);
        }
        log.info("ToolRegistry 初始化完成，已注册 {} 个工具: {}（写操作负面清单 {} 项）",
                tools.size(), tools.keySet(), WRITE_TOOL_DENYLIST.size());
    }

    public Optional<Tool> get(String toolId) {
        return Optional.ofNullable(tools.get(toolId));
    }

    public List<ToolSpec> listAll() {
        return tools.values().stream().map(Tool::spec).toList();
    }

    public int size() {
        return tools.size();
    }
}
