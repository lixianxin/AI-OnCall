import os

filepath = r"D:\AI-OnCall\ai-service\src\main\java\com\oncall\ai\service\DocumentAgentOrchestrator.java"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: topScore always uses real BM25 value, not forced to 0
old1 = """        double topScore = hit ? toolResult.getBm25MaxScore() : 0.0;
        knowledgeMetrics.recordSearch(hit, elapsedMs, topScore);"""

new1 = """        // Always use real BM25 score for metrics; hit flag controls only prompt strategy
        double topScore = toolResult.getBm25MaxScore();
        knowledgeMetrics.recordSearch(hit, elapsedMs, topScore);"""

content = content.replace(old1, new1)

# Fix 2: relevantDocCount uses sourceItems size instead of duplicate search
old2 = """        // Count actual relevant docs (score >= 0.2)
        long relevantDocCount = tooShort ? 0 : documentStore.searchWithScores(message, TOP_K).stream()
                .filter(r -> r.getScore() >= 0.5)
                .count();"""

new2 = """        // Count relevant docs from already-built sourceItems (same data source, no re-search)
        long relevantDocCount = sourceItems.size();"""

content = content.replace(old2, new2)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("P0-1 done: topScore decoupled from hit, relevantDocCount from sourceItems")
