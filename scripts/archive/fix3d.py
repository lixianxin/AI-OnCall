import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

idx = content.find("members.chunked")
if idx >= 0:
    start = max(0, idx - 200)
    end = min(len(content), idx + 400)
    chunk = content[start:end]
    print(f"Found at {idx}")
    print(chunk)
else:
    print("members.chunked not found")
