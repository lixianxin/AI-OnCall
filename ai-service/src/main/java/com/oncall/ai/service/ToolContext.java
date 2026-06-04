package com.oncall.ai.service;

public class ToolContext {
    private final String query;
    private final String conversationId;
    private final DocumentStore documentStore;

    public ToolContext(String query, String conversationId, DocumentStore documentStore) {
        this.query = query;
        this.conversationId = conversationId;
        this.documentStore = documentStore;
    }

    public String getQuery() { return query; }
    public String getConversationId() { return conversationId; }
    public DocumentStore getDocumentStore() { return documentStore; }
}