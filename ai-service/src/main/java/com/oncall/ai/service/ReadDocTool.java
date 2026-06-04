package com.oncall.ai.service;

import java.util.List;
import java.util.stream.Collectors;

public class ReadDocTool implements ToolExecutor {

    @Override
    public ToolResult execute(ToolContext context) {
        String query = context.getQuery();
        DocumentStore docStore = context.getDocumentStore();

        // Extract doc name from query (e.g. "读取 tab-manifest.md" -> "tab-manifest.md")
        String docName = extractDocName(query);
        
        // Find chunks matching the doc name
        List<DocumentChunk> allChunks = docStore.getChunks();
        List<DocumentChunk> matched = allChunks.stream()
            .filter(c -> c.getSource().toLowerCase().contains(docName.toLowerCase()))
            .collect(Collectors.toList());

        if (matched.isEmpty()) {
            return new ToolResult(
                "read", "done",
                "未找到文档：" + docName,
                List.of(),
                "文档 " + docName + " 在知识库中不存在。",
                0.0, 0.0
            );
        }

        // Build full document content
        String detailData = matched.stream()
            .map(c -> "## " + c.getSource() + "\n\n" + c.getContent())
            .collect(Collectors.joining("\n\n---\n\n"));

        String summary = "已读取 " + docName + "（" + matched.size() + " 个章节）";

        return new ToolResult(
            "read", "done",
            summary,
            List.of(docName),
            detailData,
            0.95, 0.0
        );
    }

    private String extractDocName(String query) {
        // Try to find .md pattern in query
        String[] words = query.split("[\\s,，]+");
        for (String word : words) {
            word = word.trim();
            if (word.endsWith(".md")) return word;
        }
        // Fallback: use the whole query as doc name
        return query.trim();
    }
}
