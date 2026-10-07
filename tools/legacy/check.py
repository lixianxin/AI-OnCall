import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    lines = f.readlines()

print(f"Total lines: {len(lines)}")

# Check around ParticipantSelector
for i, line in enumerate(lines):
    if "members.chunked" in line:
        start = max(0, i - 8)
        end = min(len(lines), i + 18)
        for j in range(start, end):
            print(f"{j+1}: {lines[j]}", end="")
        break
