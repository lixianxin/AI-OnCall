import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# The escaped text decodes to actual Chinese, so search for the actual characters
old = """        if (members.isEmpty()) {
            Text(
                text = "当前范围下暂无可选参与人。",
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

new = """        if (members.isEmpty()) {
            Text(
                text = "当前范围下暂无可选参与人。",
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

if old in content:
    content = content.replace(old, new)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Done: fixed ParticipantSelector")
else:
    print("NOT FOUND with decoded text either")
    # Print actual content around members.chunked
    idx = content.find("members.chunked")
    if idx >= 0:
        s = max(0, idx - 300)
        e = min(len(content), idx + 350)
        print("Actual file content:")
        print(repr(content[s:e]))
