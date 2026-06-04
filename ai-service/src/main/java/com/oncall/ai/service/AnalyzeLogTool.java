package com.oncall.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnalyzeLogTool implements ToolExecutor {

    private static final List<LogPattern> PATTERNS = new ArrayList<>();

    static {
        PATTERNS.add(new LogPattern(
            "NullPointerException",
            Pattern.compile("NullPointerException|NPE|null pointer", Pattern.CASE_INSENSITIVE),
            "troubleshooting/nullpointer.md",
            "\u5bf9\u8c61\u5f15\u7528\u4e3a null\u3002\u5e38\u89c1\u539f\u56e0\uff1a\u53d8\u91cf\u672a\u521d\u59cb\u5316\u3001\u65b9\u6cd5\u8fd4\u56de null\u672a\u5224\u7a7a\u3001Map/List \u4e2d\u4e0d\u5b58\u5728\u7684 key\u3002"
        ));
        PATTERNS.add(new LogPattern(
            "SSLException",
            Pattern.compile("SSLException|SSLHandshakeException|SSL.*certificate|cert.*verify", Pattern.CASE_INSENSITIVE),
            "troubleshooting/ssl-error.md",
            "SSL/TLS \u63e1\u624b\u5931\u8d25\u3002\u5e38\u89c1\u539f\u56e0\uff1a\u81ea\u7b7e\u540d\u8bc1\u4e66\u4e0d\u88ab\u4fe1\u4efb\u3001\u8bc1\u4e66\u5df2\u8fc7\u671f\u3001\u57df\u540d\u4e0e\u8bc1\u4e66\u4e0d\u5339\u914d\u3002"
        ));
        PATTERNS.add(new LogPattern(
            "ConnectTimeout",
            Pattern.compile("ConnectTimeoutException|Connection refused|ConnectException|no route to host", Pattern.CASE_INSENSITIVE),
            "troubleshooting/connect-timeout.md",
            "TCP \u8fde\u63a5\u5efa\u7acb\u8d85\u65f6\u6216\u88ab\u62d2\u7edd\u3002\u5e38\u89c1\u539f\u56e0\uff1a\u670d\u52a1\u7aef\u672a\u542f\u52a8\u3001\u9632\u706b\u5899\u62e6\u622a\u3001IP/\u7aef\u53e3\u914d\u7f6e\u9519\u8bef\u3002"
        ));
        PATTERNS.add(new LogPattern(
            "SocketTimeout",
            Pattern.compile("SocketTimeoutException|Read timed out|ReadTimeout", Pattern.CASE_INSENSITIVE),
            "troubleshooting/socket-timeout.md",
            "TCP \u8fde\u63a5\u5df2\u5efa\u7acb\u4f46\u670d\u52a1\u7aef\u672a\u5728\u8d85\u65f6\u65f6\u95f4\u5185\u8fd4\u56de\u6570\u636e\u3002\u5e38\u89c1\u539f\u56e0\uff1aLLM \u63a8\u7406\u8fc7\u6162\u3001readTimeout \u8fc7\u77ed\u3002"
        ));
        PATTERNS.add(new LogPattern(
            "SQLException",
            Pattern.compile("SQLException|SQL Error|DataIntegrityViolation|ConstraintViolation|Deadlock|Duplicate entry", Pattern.CASE_INSENSITIVE),
            "troubleshooting/sql-exception.md",
            "\u6570\u636e\u5e93\u64cd\u4f5c\u5931\u8d25\u3002\u5e38\u89c1\u539f\u56e0\uff1aSQL \u8bed\u6cd5\u9519\u8bef\u3001\u552f\u4e00\u7ea6\u675f\u51b2\u7a81\u3001\u6b7b\u9501\u3001\u8fde\u63a5\u6c60\u8017\u5c3d\u3002"
        ));
        PATTERNS.add(new LogPattern(
            "\u9ed1\u5c4f",
            Pattern.compile("\u9ed1\u5c4f|\u6a21\u62df\u5668|black.?screen|emulator.*display", Pattern.CASE_INSENSITIVE),
            "troubleshooting/black-screen.md",
            "Android \u6a21\u62df\u5668\u9ed1\u5c4f\u3002\u5e38\u89c1\u539f\u56e0\uff1aGPU \u6e32\u67d3\u6a21\u5f0f\u4e0d\u517c\u5bb9\u3001\u5feb\u7167\u635f\u574f\u3002\u89e3\u51b3\uff1a\u5207\u6362\u8f6f\u4ef6\u6e32\u67d3\u6216 Wipe Data\u3002"
        ));
        PATTERNS.add(new LogPattern(
            "SSE\u8fde\u63a5",
            Pattern.compile("SSE|sse|\u6d41\u65ad\u5f00|\u56de\u7b54\u4e2d\u65ad|\u8fde\u63a5\u65ad\u5f00", Pattern.CASE_INSENSITIVE),
            "troubleshooting/sse-connection.md",
            "SSE \u8fde\u63a5\u65ad\u5f00\u3002\u5e38\u89c1\u539f\u56e0\uff1aOkHttp readTimeout \u8fc7\u77ed\u3001DeepSeek API Key \u5931\u6548\u3001\u670d\u52a1\u7aef\u672a\u542f\u52a8\u3002"
        ));
        PATTERNS.add(new LogPattern(
            "DeepSeek API Error",
            Pattern.compile("DeepSeek|deepseek|API.*key|api.?key|401|api.error", Pattern.CASE_INSENSITIVE),
            "troubleshooting/deepseek-error.md",
            "DeepSeek API \u8c03\u7528\u5931\u8d25\u3002\u5e38\u89c1\u539f\u56e0\uff1aAPI Key \u65e0\u6548\u6216\u8fc7\u671f\u3001\u8d26\u6237\u4f59\u989d\u4e0d\u8db3\u3001\u8bf7\u6c42\u9891\u7387\u8d85\u9650\u3002"
        ));
    }

    @Override
    public ToolResult execute(ToolContext context) {
        String logText = context.getQuery();

        List<String> matchedSources = new ArrayList<>();
        StringBuilder analysis = new StringBuilder();
        analysis.append("\u3010\u65e5\u5fd7\u5206\u6790\u7ed3\u679c\u3011\n\n");

        boolean found = false;
        for (LogPattern pattern : PATTERNS) {
            if (pattern.matcher().matcher(logText).find()) {
                found = true;
                matchedSources.add(pattern.source());
                analysis.append("### ").append(pattern.name()).append("\n\n");
                analysis.append(pattern.description()).append("\n\n");
                analysis.append("\u76f8\u5173\u6587\u6863\uff1a").append(pattern.source()).append("\n\n---\n\n");
            }
        }

        double confidence;
        if (found) {
            confidence = 0.85;
        } else {
            DocumentStore docStore = context.getDocumentStore();
            List<SearchResult> results = docStore.searchWithScores(logText, 3);
            confidence = SearchTool.computeConfidence(results, logText);
            if (!results.isEmpty()) {
                analysis.append("\u68c0\u7d22\u5230\u4ee5\u4e0b\u76f8\u5173\u6587\u6863\u7247\u6bb5\uff1a\n\n");
                for (SearchResult r : results) {
                    matchedSources.add(r.getChunk().getSource());
                    analysis.append("> \u3010").append(r.getChunk().getSource()).append("\u3011\n");
                    analysis.append(r.getChunk().getContent()).append("\n\n");
                }
            } else {
                analysis.append("\u672a\u5728\u9519\u8bef\u77e5\u8bc6\u5e93\u4e2d\u627e\u5230\u5339\u914d\u7684\u5df2\u77e5\u9519\u8bef\u6a21\u5f0f\u3002\n\n");
            }
        }

        // Add LLM dynamic analysis instruction
        analysis.append("\u3010\u8bf7DeepSeek\u52a8\u6001\u5206\u6790\u3011\n");
        analysis.append("\u8bf7\u57fa\u4e8e\u4ee5\u4e0a\u5206\u6790\u548c\u7528\u6237\u63d0\u4f9b\u7684\u5f02\u5e38\u5806\u6808\uff0c\u751f\u6210\u5982\u4e0b\u7ed3\u6784\u5316\u56de\u7b54\uff1a\n");
        analysis.append("1. \u95ee\u9898\u5b9a\u4f4d\uff1a\u5177\u4f53\u54ea\u4e00\u884c\u4ee3\u7801\u51fa\u73b0\u95ee\u9898\n");
        analysis.append("2. \u6839\u56e0\u5206\u6790\uff1a\u4e3a\u4ec0\u4e48\u4f1a\u89e6\u53d1\u8be5\u9519\u8bef\n");
        analysis.append("3. \u4fee\u590d\u65b9\u6848\uff1a\u7ed9\u51fa\u5177\u4f53\u7684\u4ee3\u7801\u4fee\u6539\u5efa\u8bae\n");
        analysis.append("4. \u9884\u9632\u63aa\u65bd\uff1a\u5982\u4f55\u907f\u514d\u7c7b\u4f3c\u9519\u8bef");

        String summary;
        if (confidence >= 0.7) {
            summary = "\u5df2\u5b8c\u6210\u65e5\u5fd7\u5206\u6790\uff0c\u5339\u914d\u5230 " + matchedSources.size() + " \u4e2a\u5df2\u77e5\u9519\u8bef\u6a21\u5f0f";
        } else {
            summary = "\u65e5\u5fd7\u5206\u6790\u5339\u914d\u5ea6\u8f83\u4f4e\uff08" + String.format("%.0f%%", confidence * 100) + "\uff09\uff0c\u5c06\u501f\u52a9 AI \u80fd\u529b\u8865\u5145\u56de\u7b54";
        }

        return new ToolResult(
            "analyze_log",
            "done",
            summary,
            matchedSources.stream().distinct().collect(Collectors.toList()),
            analysis.toString(),
            confidence
        );
    }

    private record LogPattern(String name, Pattern matcher, String source, String description) {}
}
