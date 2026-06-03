package com.oncall.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"type", "tool"})
public record ToolEvent(String tool) implements ChatEvent {

    @Override
    public String type() {
        return "tool";
    }

    /** 预定义的工具类型常量 */
    public static final String SEARCH = "search";
    public static final String READ = "read";
    public static final String ANALYZE = "analyze";
    public static final String GENERATE = "generate";
}
