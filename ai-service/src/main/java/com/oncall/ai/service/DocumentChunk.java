package com.oncall.ai.service;

/**
 * 文档切块，用于 RAG 检索。
 * 每个 Chunk 包含一段文本及其来源元信息。
 */
public class DocumentChunk {

    private final String id;
    private final String content;
    private final String source;
    private final int index;

    public DocumentChunk(String id, String content, String source, int index) {
        this.id = id;
        this.content = content;
        this.source = source;
        this.index = index;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public String getSource() { return source; }
    public int getIndex() { return index; }

    @Override
    public String toString() {
        return "DocumentChunk{id='" + id + "', source='" + source + "', len=" + content.length() + "}";
    }
}
