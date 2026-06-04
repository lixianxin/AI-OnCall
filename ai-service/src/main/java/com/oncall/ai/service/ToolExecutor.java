package com.oncall.ai.service;

public interface ToolExecutor {
    ToolResult execute(ToolContext context);
}