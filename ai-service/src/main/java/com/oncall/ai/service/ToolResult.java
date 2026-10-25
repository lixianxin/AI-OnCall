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
    /**
     * 工具内部产出的原始检索结果（仅检索类工具填充，其他工具为 null）。
     * 编排层直接复用该结果构建 SourcesEvent / 命中判定，避免用相同参数重复检索。
     */
    private final List<SearchResult> searchResults;

    public ToolResult(String toolName, String status, String summary,
                      List<String> sources, String detailData, double confidence) {
        this(toolName, status, summary, sources, detailData, confidence, 0.0);
    }

    public ToolResult(String toolName, String status, String summary,
                      List<String> sources, String detailData, double confidence, double bm25MaxScore) {
        this(toolName, status, summary, sources, detailData, confidence, bm25MaxScore, null);
    }

    public ToolResult(String toolName, String status, String summary,
                      List<String> sources, String detailData, double confidence,
                      double bm25MaxScore, List<SearchResult> searchResults) {
        this.toolName = toolName;
        this.status = status;
        this.summary = summary;
        this.sources = sources;
        this.detailData = detailData;
        this.confidence = confidence;
        this.bm25MaxScore = bm25MaxScore;
        this.searchResults = searchResults;
    }

    public String getToolName() { return toolName; }
    public String getStatus() { return status; }
    public String getSummary() { return summary; }
    public List<String> getSources() { return sources; }
    public String getDetailData() { return detailData; }
    public double getConfidence() { return confidence; }
    public double getBm25MaxScore() { return bm25MaxScore; }
    /** 检索类工具返回内部检索结果；非检索工具或降级结果返回 null */
    public List<SearchResult> getSearchResults() { return searchResults; }
}
