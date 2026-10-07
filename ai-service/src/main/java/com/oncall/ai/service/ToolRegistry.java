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

    public ToolRegistry(List<Tool> toolBeans) {
        for (Tool tool : toolBeans) {
            ToolSpec spec = tool.spec();
            if (tools.containsKey(spec.getName())) {
                throw new IllegalStateException("重复注册的工具 id: " + spec.getName());
            }
            tools.put(spec.getName(), tool);
        }
        log.info("ToolRegistry 初始化完成，已注册 {} 个工具: {}", tools.size(), tools.keySet());
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
