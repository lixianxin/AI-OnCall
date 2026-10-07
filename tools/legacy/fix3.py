with open(r'D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# We need to fix lines 4964-4988 (0-indexed: 4963-4987)
# The bug: } else if (centerTitle) follows a forEach, not an if
# Fix: wrap the forEach in an else block, remove the centerTitle part

new_lines = []
i = 0
while i < len(lines):
    line = lines[i]
    # Detect the problematic block
    if i == 4963:  # 0-indexed, corresponds to line 4964: if (members.isEmpty()) {
        # Collect all lines until we find the participant selector function end
        block_lines = []
        j = i
        while j < len(lines):
            block_lines.append(lines[j])
            if '}' in lines[j] and 'private fun ConfirmActionCard' in (lines[j+1] if j+1 < len(lines) else ''):
                break
            j += 1
        
        # Rebuild the block correctly
        # Keep lines up to the forEach, but restructure
        # Replace the entire broken block
        fixed = '''        if (members.isEmpty()) {
            Text(
                text = "\u65e5\u7a0b\u53c2\u4e0e\u4eba",
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
        }
    }
}

'''
        new_lines.append(fixed)
        i = j + 1
        continue
    new_lines.append(line)
    i += 1

with open(r'D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print('Fix 3 done')
