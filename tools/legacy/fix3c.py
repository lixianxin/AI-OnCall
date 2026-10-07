import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Find the exact text around the broken code
idx = content.find("else if (centerTitle)")
if idx >= 0:
    start = max(0, idx - 650)
    end = min(len(content), idx + 100)
    chunk = content[start:end]
    print("FOUND! Context:")
    print(chunk)
    print("---END---")
else:
    print("else if (centerTitle) not found")
