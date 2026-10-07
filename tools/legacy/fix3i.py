import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Replace the broken block in ParticipantSelector
old_pattern = """        if (members.isEmpty()) {
            Text(
                text = "\u5f53\u524d\u8303\u56f4\u4e0b\u6682\u65e0\u53ef\u9009\u53c2\u4e0e\u4eba\u3002",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        members.chunked(size = 2).forEach { rowMembers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowMembers.forEach { member ->
                    FilterChipButton(
                        text = member.displayMemberName(),
                        selected = selectedUserIds.contains(member.userId),
                        onClick = { onToggle(member.userId) }
                    )
                }
            }
        } else if (centerTitle) {
            Box(modifier = Modifier.size(size = 36.dp))
        }"""

new_pattern = """        if (members.isEmpty()) {
            Text(
                text = "\u5f53\u524d\u8303\u56f4\u4e0b\u6682\u65e0\u53ef\u9009\u53c2\u4e0e\u4eba\u3002",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        } else {
            members.chunked(size = 2).forEach { rowMembers ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowMembers.forEach { member ->
                        FilterChipButton(
                            text = member.displayMemberName(),
                            selected = selectedUserIds.contains(member.userId),
                            onClick = { onToggle(member.userId) }
                        )
                    }
                }
            }
        }"""

if old_pattern in content:
    content = content.replace(old_pattern, new_pattern)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Done: fixed ParticipantSelector")
else:
    print("Pattern not found, trying alternative...")
    # Try with indented version
    idx = content.find("\u5f53\u524d\u8303\u56f4\u4e0b\u6682\u65e0\u53ef\u9009\u53c2\u4e0e\u4eba\u3002")
    if idx >= 0:
        start = max(0, idx - 50)
        end = min(len(content), idx + 450)
        print("Found text, context:")
        print(repr(content[start:end]))
