package com.oncall.ai.service;

import java.util.List;
import java.util.stream.Collectors;

public class GenerateCodeTool implements ToolExecutor {

    private static final int TOP_K = 5;

    @Override
    public ToolResult execute(ToolContext context) {
        String query = context.getQuery();
        DocumentStore docStore = context.getDocumentStore();

        // Search code-template docs and android-sdk docs
        List<SearchResult> results = docStore.searchWithScores(query, TOP_K);
        double confidence = SearchTool.computeConfidence(results, query);
        double bm25MaxScore = results.isEmpty() ? 0.0 : results.get(0).getScore();

        if (results.isEmpty()) {
            return new ToolResult(
                "generate", "done",
                "未找到相关代码模板",
                List.of(),
                "请提供更具体的需求描述（如 Controller、Repository、SSE 接口等）。",
                0.0, 0.0
            );
        }

        List<String> sources = results.stream()
            .map(r -> r.getChunk().getSource())
            .distinct()
            .collect(Collectors.toList());

        String detailData = "以下是符合项目规范的代码模板，请严格基于模板生成完整代码：\n\n"
            + results.stream()
                .map(r -> "> 【" + r.getChunk().getSource() + "】\n" + r.getChunk().getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        String summary;
        if (confidence >= 0.7) {
            summary = "已检索到 " + results.size() + " 个相关代码模板，正在生成代码…";
        } else {
            summary = "检索到 " + results.size() + " 个参考模板（BM25最高得分：" + String.format("%.2f", bm25MaxScore) + "），正在生成…";
        }

        return new ToolResult(
            "generate", "done",
            summary,
            sources,
            detailData,
            confidence,
            bm25MaxScore
        );
    }
}
