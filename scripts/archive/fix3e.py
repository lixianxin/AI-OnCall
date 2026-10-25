import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Find all occurrences of "centerTitle"
pos = 0
count = 0
while True:
    idx = content.find("centerTitle", pos)
    if idx < 0:
        break
    count += 1
    start = max(0, idx - 30)
    end = min(len(content), idx + 30)
    print(f"Occurrence {count} at {idx}: ...{content[start:end]}...")
    pos = idx + 1
