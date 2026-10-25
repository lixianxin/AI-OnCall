import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# The broken code in ParticipantSelector
old_block = """        if (members.isEmpty()) {
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

new_block = """        if (members.isEmpty()) {
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

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Done: fixed ParticipantSelector syntax")
else:
    # Try to find it
    idx = content.find("当前范围下暂无可选参与人。")
    if idx >= 0:
        print(f"Found at index {idx}, context:")
        print(content[idx:idx+500])
    else:
        print("ERROR: not found, trying escaped version")
        idx = content.find("\\u5f53\\u524d\\u8303\\u56f4\\u4e0b")
        if idx >= 0:
            print(f"Found escaped at {idx}")
        else:
            # Try with raw strings
            for keyword in ["members.chunked", "else if (centerTitle)"]:
                idx = content.find(keyword)
                if idx >= 0:
                    print(f"Found '{keyword}' at {idx}")
                    print(content[max(0,idx-100):idx+200])
