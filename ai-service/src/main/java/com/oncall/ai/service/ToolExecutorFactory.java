package com.oncall.ai.service;

import com.oncall.ai.model.IntentEvent;
import com.oncall.ai.model.ToolEvent;

/**
 * @deprecated 由 {@link ToolRegistry}（Spring 自动收集 Tool Bean）取代。
 * 保留仅为兼容历史调用；新代码应通过 {@link ToolExecutor} 治理层执行工具。
 */
@Deprecated
public class ToolExecutorFactory {

    public static Tool create(String toolName) {
        return switch (toolName) {
            case ToolEvent.SEARCH -> new SearchTool();
            case ToolEvent.ANALYZE_LOG -> new AnalyzeLogTool();
            case ToolEvent.READ -> new ReadDocTool();
            case ToolEvent.GENERATE -> new GenerateCodeTool();   // GenerateCodeTool (falls back to search for templates)
            default -> new SearchTool();                   // Default fallback
        };
    }
}