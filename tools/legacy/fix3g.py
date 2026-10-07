import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Find the exact block from "if (members.isEmpty())" to the function end after "else if (centerTitle)"
idx_start = content.find("if (members.isEmpty())")
idx_end = content.find("@Composable", idx_start)
if idx_end < 0:
    idx_end = content.find("private fun ", idx_start)

print(f"if (members.isEmpty()) at {idx_start}")
print(f"Next function/annotation at {idx_end}")

# Get the broken block
broken = content[idx_start:idx_end]
print("BROKEN BLOCK:")
print(broken)
print("---END---")
