package com.oncall.ai.service;

import java.util.List;

public class ToolResult {
    private final String toolName;
    private final String status;
    private final String summary;
    private final List<String> sources;
    private final String detailData;
    private final double confidence;
    private final double bm25MaxScore;

    public ToolResult(String toolName, String status, String summary,
                      List<String> sources, String detailData, double confidence) {
        this(toolName, status, summary, sources, detailData, confidence, 0.0);
    }

    public ToolResult(String toolName, String status, String summary,
                      List<String> sources, String detailData, double confidence, double bm25MaxScore) {
        this.toolName = toolName;
        this.status = status;
        this.summary = summary;
        this.sources = sources;
        this.detailData = detailData;
        this.confidence = confidence;
        this.bm25MaxScore = bm25MaxScore;
    }

    public String getToolName() { return toolName; }
    public String getStatus() { return status; }
    public String getSummary() { return summary; }
    public List<String> getSources() { return sources; }
    public String getDetailData() { return detailData; }
    public double getConfidence() { return confidence; }
    public double getBm25MaxScore() { return bm25MaxScore; }
}
