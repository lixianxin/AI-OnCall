package com.oncall.ai.controller;

import com.oncall.ai.service.AgentMetrics;
import com.oncall.ai.service.ToolRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维端点：健康检查 + 可观测性指标
 *
 * - GET /health  ：存活探测 + 已注册工具数
 * - GET /metrics ：Tool 调用数 / 错误数 / 平均延迟 / 重试次数 / token 用量
 *
 * 不依赖 Prometheus，内存指标足够支撑"瓶颈在哪里"这类面试追问。
 */
@RestController
public class MetricsController {

    private final AgentMetrics metrics;
    private final ToolRegistry registry;

    public MetricsController(AgentMetrics metrics, ToolRegistry registry) {
        this.metrics = metrics;
        this.registry = registry;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("tools_registered", registry.size());
        return m;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tools_registered", registry.size());
        m.put("tool_specs", registry.listAll());
        m.put("metrics", metrics.snapshot());
        return m;
    }
}
