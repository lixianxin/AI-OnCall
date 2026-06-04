import os

filepath = r"D:\AI-OnCall\ai-service\src\main\java\com\oncall\ai\controller\ChatController.java"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fix the corrupted line 64 - replace with ASCII-safe JSON
old = '.data("{\"type\":\"error\",\"code\":\"DUPLICATE_MESSAGE\",\"delta\":\"消息已处理，跳过重复请求\"}")'
new = '.data("{\\"type\\":\\"error\\",\\"code\\":\\"DUPLICATE_MESSAGE\\",\\"delta\\":\\"Duplicate message skipped\\"}")'

# Try to find and fix the corrupted line
import re
# The corrupted line might have garbled chars - find by pattern
pattern = r'\.data\(".*DUPLICATE_MESSAGE.*"\)'
match = re.search(pattern, content)
if match:
    content = content[:match.start()] + new + content[match.end():]
    print("Fixed corrupted JSON line")
else:
    print("Pattern not found, searching for DUPLICATE_MESSAGE...")
    idx = content.find("DUPLICATE_MESSAGE")
    if idx >= 0:
        print(f"Found at {idx}: {content[idx-10:idx+100]}")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
