import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenWorkbenchPage.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Add 2 more closing braces before CategorySelector
# Currently: "    }\n}\n\n@Composable\nprivate fun CategorySelector("
# Need:       "    }\n    }\n}\n}\n\n@Composable\nprivate fun CategorySelector("
old = "    }\n}\n\n@Composable\nprivate fun CategorySelector("
new = "    }\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun CategorySelector("

content = content.replace(old, new)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

# Verify depth
lines = content.split('\n')
depth = 0
for line in lines:
    for ch in line:
        if ch == '{': depth += 1
        if ch == '}': depth -= 1
print(f"Depth after fix: {depth}")
