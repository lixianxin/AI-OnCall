package com.oncall.ai.service;

public class ToolContext {
    private final String query;
    private final String conversationId;
    private final DocumentStore documentStore;
    /** 写操作人工确认标记。写操作工具（ToolSpec.writeOperation=true）执行前必须为 true（ToolExecutor 强制） */
    private final boolean writeConfirmed;

    public ToolContext(String query, String conversationId, DocumentStore documentStore) {
        this(query, conversationId, documentStore, false);
    }

    public ToolContext(String query, String conversationId, DocumentStore documentStore, boolean writeConfirmed) {
        this.query = query;
        this.conversationId = conversationId;
        this.documentStore = documentStore;
        this.writeConfirmed = writeConfirmed;
    }

    public String getQuery() { return query; }
    public String getConversationId() { return conversationId; }
    public DocumentStore getDocumentStore() { return documentStore; }
    public boolean isWriteConfirmed() { return writeConfirmed; }
}