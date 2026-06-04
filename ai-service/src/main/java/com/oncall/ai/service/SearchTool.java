package com.oncall.ai.service;

import java.util.List;
import java.util.stream.Collectors;

public class SearchTool implements ToolExecutor {

    private static final int TOP_K = 5;

    /**
     * Compute KB match confidence 0.0 ~ 1.0 based on BM25 scores.
     *
     * Algorithm:
     * - No results → 0.0
     * - Score distribution analysis:
     *   - maxScore >= 6.0 → 1.0 (strong match)
     *   - maxScore >= 3.0 → 0.6 + (maxScore - 3.0) / 7.5 (0.6 ~ 1.0)
     *   - maxScore >= 1.0 → 0.3 + (maxScore - 1.0) / 10.0 (0.3 ~ 0.6)
     *   - maxScore < 1.0 → maxScore / 3.33 (0.0 ~ 0.3)
     * - Title match bonus: if best result title contains query keywords, add 0.15
     * - Source diversity bonus: if >= 2 unique sources, add 0.1
     */
    public static double computeConfidence(List<SearchResult> results, String query) {
        if (results == null || results.isEmpty()) return 0.0;

        double maxScore = results.get(0).getScore();

        // Score-based confidence
        double scoreConfidence;
        if (maxScore >= 6.0) {
            scoreConfidence = 1.0;
        } else if (maxScore >= 3.0) {
            scoreConfidence = 0.6 + (maxScore - 3.0) / 7.5;
        } else if (maxScore >= 1.0) {
            scoreConfidence = 0.3 + (maxScore - 1.0) / 10.0;
        } else {
            scoreConfidence = maxScore / 3.33;
        }

        // Title match bonus: check if top chunk title contains query keywords
        double titleBonus = 0.0;
        String topContent = results.get(0).getChunk().getContent();
        if (topContent.startsWith("\u3010")) {
            int close = topContent.indexOf("\u3011");
            if (close > 1) {
                String title = topContent.substring(1, close).toLowerCase();
                String queryLower = query.toLowerCase();
                for (String word : queryLower.split("\\s+")) {
                    if (word.length() >= 2 && title.contains(word)) {
                        titleBonus = 0.15;
                        break;
                    }
                }
                // Also check Chinese chars
                for (char c : queryLower.toCharArray()) {
                    if (c >= 0x4e00 && c <= 0x9fff && title.indexOf(c) >= 0) {
                        titleBonus = 0.15;
                        break;
                    }
                }
            }
        }

        // Source diversity bonus
        double diversityBonus = 0.0;
        long uniqueSources = results.stream()
                .map(r -> r.getChunk().getSource())
                .distinct()
                .count();
        if (uniqueSources >= 2) diversityBonus = 0.1;

        double confidence = Math.min(1.0, scoreConfidence + titleBonus + diversityBonus);
        return confidence;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        String query = context.getQuery();
        DocumentStore docStore = context.getDocumentStore();

        List<SearchResult> results = docStore.searchWithScores(query, TOP_K);
        double confidence = computeConfidence(results, query);
        double bm25MaxScore = results.isEmpty() ? 0.0 : results.get(0).getScore();

        if (results.isEmpty()) {
            return new ToolResult(
                "search",
                "done",
                "知识库中未找到相关文档",
                List.of(),
                "",
                confidence,
                0.0
            );
        }


        // Build source entries with score info
        List<String> sources = results.stream()
            .map(r -> r.getChunk().getSource()
                + " [score: " + String.format("%.2f", r.getScore())
                + ", bm25: " + String.format("%.2f", bm25MaxScore) + "]")
            .distinct()
            .collect(Collectors.toList());

        // Build detail data for LLM
        String detailData = results.stream()
            .map(r -> "> \u3010" + r.getChunk().getSource() + "\u3011\n" + r.getChunk().getContent())
            .collect(Collectors.joining("\n\n---\n\n"));

        String summary;
        if (confidence >= 0.7) {
            summary = "已检索到 " + results.size() + " 条相关文档片段（BM25最高得分：" + String.format("%.2f", bm25MaxScore) + "）";
        } else {
            summary = "知识库中匹配度较低（BM25最高得分：" + String.format("%.2f", bm25MaxScore) + "），将借助 AI 能力补充回答";
        }


        return new ToolResult(
            "search",
            "done",
            summary,
            sources,
            detailData,
            confidence,
            bm25MaxScore
        );
    }
}