import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenOnCallPage.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: Remove duplicate @Composable
content = content.replace("@Composable\n@Composable\nprivate fun SourceMessageCard", "@Composable\nprivate fun SourceMessageCard")

# Fix 2: Add @Composable back to ToolMessageCard (it was lost in the replacement)
content = content.replace("\nprivate fun ToolMessageCard(message: OnCallMessageUi.Tool) {", "\n@Composable\nprivate fun ToolMessageCard(message: OnCallMessageUi.Tool) {")

# Fix 3: Add SourceCard branch to the when expression
old_when2 = """                    is OnCallMessageUi.Tool -> ToolMessageCard(message = message)"""
new_when2 = """                    is OnCallMessageUi.Tool -> ToolMessageCard(message = message)
                                    is OnCallMessageUi.SourceCard -> SourceMessageCard(message = message)"""
content = content.replace(old_when2, new_when2, 1)  # Only first occurrence

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed 3 issues")
