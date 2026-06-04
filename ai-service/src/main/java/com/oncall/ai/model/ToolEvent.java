package com.oncall.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class ToolEvent implements ChatEvent {

    private final String tool;
    private String status = "running";
    private String summary = "";

    public ToolEvent(String tool) {
        this.tool = tool;
    }

    public ToolEvent(String tool, String status, String summary) {
        this.tool = tool;
        this.status = status;
        this.summary = summary;
    }

    @Override
    public String type() {
        return "tool";
    }

    @JsonProperty("tool")
    public String getTool() { return tool; }

    @JsonProperty("status")
    public String getStatus() { return status; }

    @JsonProperty("summary")
    public String getSummary() { return summary; }

    public void setStatus(String status) { this.status = status; }
    public void setSummary(String summary) { this.summary = summary; }

    public static final String SEARCH = "search";
    public static final String READ = "read";
    public static final String ANALYZE_LOG = "analyze_log";
    public static final String GENERATE = "generate";
    public static final String EXPLAIN = "explain";
}
