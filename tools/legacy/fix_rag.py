import os

filepath = r"D:\AI-OnCall\ai-service\src\main\java\com\oncall\ai\service\DocumentAgentOrchestrator.java"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: Change the SYSTEM_PROMPT to remove "自行判断" and make it unconditional
old_system_prompt = '        "3. \\u81ea\\u884c\\u5224\\u65ad\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u662f\\u5426\\u4e0e\\u95ee\\u9898\\u76f8\\u5173\\uff0c\\u65e0\\u5173\\u65f6\\u6309\\u4e0b\\u65b9\\u3010\\u5173\\u952e\\u89c4\\u5219\\u3011\\u5904\\u7406\\u3002\\n" +'
new_system_prompt = '        "3. \\u5982\\u679c\\u3010\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u3011\\u4e2d\\u5305\\u542b\\u76f8\\u5173\\u4fe1\\u606f\\uff0c\\u4f60\\u5fc5\\u987b\\u4f18\\u5148\\u4f9d\\u636e\\u77e5\\u8bc6\\u5e93\\u56de\\u7b54\\u3002\\n" +'

content = content.replace(old_system_prompt, new_system_prompt)

# Fix 2: Change the prompt building to be conditional on hit/miss
old_prompt_build = """        // Unified prompt: always pass knowledge + fallback rule
        // Model decides whether knowledge base content is relevant
        String enrichedPrompt = SYSTEM_PROMPT
                + "\\n\\n\\u3010\\u68c0\\u7d22\\u6307\\u6807\\u3011\\n" + metricsLine
                + "\\n\\n\\u3010\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u3011\\n" + toolResult.getDetailData()
                + "\\n\\n\\u3010\\u5173\\u952e\\u89c4\\u5219\\u3011\\n"
                + "\\u8bf7\\u5148\\u5224\\u65ad\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u662f\\u5426\\u4e0e\\u7528\\u6237\\u95ee\\u9898\\u76f8\\u5173\\uff1a\\n"
                + "- \\u5982\\u679c\\u76f8\\u5173\\uff1a\\u4f9d\\u636e\\u77e5\\u8bc6\\u5e93\\u56de\\u7b54\\uff0c\\u5f15\\u7528\\u6765\\u6e90\\u6587\\u4ef6\\u3002\\n"
                + "- \\u5982\\u679c\\u4e0d\\u76f8\\u5173\\u6216\\u672a\\u547d\\u4e2d\\uff1a\\u8f93\\u51fa\\u3010\\u77e5\\u8bc6\\u5e93\\u672a\\u547d\\u4e2d\\u3011\\uff0c\\u7136\\u540e\\u57fa\\u4e8e\\u4f60\\u7684\\u8bad\\u7ec3\\u77e5\\u8bc6\\u56de\\u7b54\\uff0c\\u5728\\u56de\\u7b54\\u4e2d\\u6807\\u6ce8\\u300c\\u4ee5\\u4e0b\\u56de\\u7b54\\u6765\\u81ea AI \\u6a21\\u578b\\u77e5\\u8bc6\\u5e93\\uff0c\\u4ec5\\u4f9b\\u53c2\\u8003\\u300d\\u3002\\n"
                + "\\u56de\\u7b54\\u672b\\u5c3e\\u53e6\\u8d77\\u4e00\\u884c\\u8f93\\u51fa\\u4e0b\\u65b9\\u3010\\u68c0\\u7d22\\u6307\\u6807\\u3011\\u4e2d\\u7684\\u5185\\u5bb9";"""

new_prompt_build = """        // Code-determined hit/miss: tell the model what to do instead of letting it decide
        String ruleBlock;
        if (hit) {
            ruleBlock = "\\n\\n\\u3010\\u5173\\u952e\\u89c4\\u5219\\u3011\\n"
                + "\\u4ee5\\u4e0a\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u5df2\\u7ecf\\u7cfb\\u7edf\\u68c0\\u7d22\\u786e\\u8ba4\\u4e0e\\u95ee\\u9898\\u76f8\\u5173\\uff0c\\u4f60\\u5fc5\\u987b\\u4f9d\\u636e\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u56de\\u7b54\\uff0c\\u5e76\\u81ea\\u7136\\u5f15\\u7528\\u6765\\u6e90\\u6587\\u4ef6\\u540d\\u3002\\u56de\\u7b54\\u672b\\u5c3e\\u53e6\\u8d77\\u4e00\\u884c\\u8f93\\u51fa\\u4e0b\\u65b9\\u3010\\u68c0\\u7d22\\u6307\\u6807\\u3011\\u4e2d\\u7684\\u5185\\u5bb9\\u3002";
        } else {
            ruleBlock = "\\n\\n\\u3010\\u5173\\u952e\\u89c4\\u5219\\u3011\\n"
                + "\\u77e5\\u8bc6\\u5e93\\u672a\\u547d\\u4e2d\\u76f8\\u5173\\u5185\\u5bb9\\u3002\\u4f60\\u5e94\\u5f53\\uff1a\\n"
                + "1. \\u8f93\\u51fa\\u3010\\u77e5\\u8bc6\\u5e93\\u672a\\u547d\\u4e2d\\u3011\\n"
                + "2. \\u57fa\\u4e8e\\u4f60\\u7684\\u8bad\\u7ec3\\u77e5\\u8bc6\\u56de\\u7b54\\u7528\\u6237\\u95ee\\u9898\\n"
                + "3. \\u6807\\u6ce8\\u300c\\u4ee5\\u4e0b\\u56de\\u7b54\\u6765\\u81ea AI \\u6a21\\u578b\\u77e5\\u8bc6\\u5e93\\uff0c\\u4ec5\\u4f9b\\u53c2\\u8003\\u300d\\n"
                + "\\u56de\\u7b54\\u672b\\u5c3e\\u53e6\\u8d77\\u4e00\\u884c\\u8f93\\u51fa\\u4e0b\\u65b9\\u3010\\u68c0\\u7d22\\u6307\\u6807\\u3011\\u4e2d\\u7684\\u5185\\u5bb9\\u3002";
        }
        String enrichedPrompt = SYSTEM_PROMPT
                + "\\n\\n\\u3010\\u68c0\\u7d22\\u6307\\u6807\\u3011\\n" + metricsLine
                + "\\n\\n\\u3010\\u77e5\\u8bc6\\u5e93\\u5185\\u5bb9\\u3011\\n" + toolResult.getDetailData()
                + ruleBlock;"""

if old_prompt_build in content:
    content = content.replace(old_prompt_build, new_prompt_build)
    print("Fixed prompt building logic")
else:
    print("WARNING: old prompt build pattern not found")
    # Try checking what's actually there
    idx = content.find("Model decides")
    if idx >= 0:
        print(f"'Model decides' found at {idx}")
        print(repr(content[idx:idx+100]))

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("File saved")
