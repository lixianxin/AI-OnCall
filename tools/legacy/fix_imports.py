import os

filepath = r"D:\AI-OnCall\ai-service\src\main\java\com\oncall\ai\service\ConversationStore.java"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Remove duplicate ConcurrentHashMap import
old_dup = "import java.util.concurrent.ConcurrentHashMap;\nimport java.util.concurrent.ConcurrentHashMap;"
new_dup = "import java.util.concurrent.ConcurrentHashMap;"
content = content.replace(old_dup, new_dup)

# Also fix the import order - Set and Collections should be together
old_imports = "import java.util.Optional;\nimport java.util.Set;\nimport java.util.concurrent.ConcurrentHashMap;\nimport java.util.Collections;"
new_imports = "import java.util.Collections;\nimport java.util.Optional;\nimport java.util.Set;\nimport java.util.concurrent.ConcurrentHashMap;"
content = content.replace(old_imports, new_imports)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed imports")
