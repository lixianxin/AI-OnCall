package com.oncall.ai.service;

import com.oncall.ai.model.IntentEvent;
import com.oncall.ai.model.ToolEvent;

public class ToolExecutorFactory {

    public static ToolExecutor create(String toolName) {
        return switch (toolName) {
            case ToolEvent.SEARCH -> new SearchTool();
            case ToolEvent.ANALYZE_LOG -> new AnalyzeLogTool();
            case ToolEvent.READ -> new ReadDocTool();
            case ToolEvent.GENERATE -> new GenerateCodeTool();   // GenerateCodeTool (falls back to search for templates)
            default -> new SearchTool();                   // Default fallback
        };
    }
}