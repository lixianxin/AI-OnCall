import os

filepath = r"D:\AI-OnCall\ai-service\src\main\java\com\oncall\ai\service\DocumentAgentOrchestrator.java"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Inject memory context into the prompt construction
old = """        String enrichedPrompt = SYSTEM_PROMPT
                + "\\n\\n\\u3010\\u68c0\\u7d22\\u6307\\u6807\\u3011\\n" + metricsLine
                + "\\n\\n\\u3010\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u3011\\n" + toolResult.getDetailData()
                + ruleBlock;"""

new = """        // Build Memory context from Conversation (summary + recent tools + recent sources + history)
        String memoryContext = updatedConv.buildMemoryContext();

        String enrichedPrompt = SYSTEM_PROMPT
                + memoryContext
                + "\\n\\n\\u3010\\u68c0\\u7d22\\u6307\\u6807\\u3011\\n" + metricsLine
                + "\\n\\n\\u3010\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u3011\\n" + toolResult.getDetailData()
                + ruleBlock;"""

if old in content:
    content = content.replace(old, new)
    print("Memory injection added to prompt")
else:
    print("Pattern not found, checking...")
    idx = content.find("enrichedPrompt = SYSTEM_PROMPT")
    if idx >= 0:
        print(content[idx:idx+300])

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
