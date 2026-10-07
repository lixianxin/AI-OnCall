import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# The file has literal \u escapes, so search with double-backslash
old = content[content.find("if (members.isEmpty())"):content.find("else if (centerTitle)")+len("else if (centerTitle) {\n            Box(modifier = Modifier.size(size = 36.dp))\n        }")]

# Find the chunk more precisely
idx_if = content.find("if (members.isEmpty())", content.find("members.chunked") - 500)
idx_end = content.find("else if (centerTitle)", idx_if)
# Find the matching closing
idx_close = content.find("}", idx_end + 50)

print(f"idx_if={idx_if}, idx_end={idx_end}, idx_close={idx_close}")

# Extract the full broken block
broken = content[idx_if:idx_close+1]
print("BROKEN:")
print(broken)

# Now construct the replacement
# The if block is from idx_if to just before members.chunked
# The else if part is from "} else if (centerTitle)" to its closing "}"
chunked_start = content.find("members.chunked", idx_if)
if_end = content.rfind("}", idx_if, chunked_start)

fixed = content[idx_if:if_end+1] + " else {" + content[chunked_start:idx_end] + "\n            }\n        }"

print("\nFIXED:")
print(fixed)

# Now apply replacement
content = content.replace(broken, fixed)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("\nDone: fixed ParticipantSelector")
