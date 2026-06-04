import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenWorkbenchPage.kt"

with open(filepath, "r", encoding="utf-8") as f:
    lines = f.readlines()

# Remove lines 1062-1089 (0-indexed: 1061-1088) - the duplicate ContainerIconOptionButton body
# These are the lines between ContainerTypeSelector's forEach closing and the CategorySelector function
# Keep lines before 1062 and after 1089

new_lines = lines[:1061] + lines[1089:]

with open(filepath, "w", encoding="utf-8") as f:
    f.writelines(new_lines)

print(f"Removed {1089-1061} lines of duplicate code. New total: {len(new_lines)}")
