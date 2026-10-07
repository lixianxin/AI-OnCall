import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

placeholder = """

@Composable
private fun CalendarPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        SectionCard(
            title = "\u65e5\u7a0b\u7ba1\u7406",
            body = "\u65e5\u7a0b\u7ba1\u7406\u529f\u80fd\u4ecd\u5728\u63a5\u5165\u4e2d\uff0c\u540e\u7eed\u4f1a\u5c55\u793a\u56e2\u961f\u65e5\u7a0b\u3001\u4f1a\u8bae\u5b89\u6392\u548c\u63d0\u9192\u529f\u80fd\u3002"
        )
    }
}

"""

old = "\n@Composable\nprivate fun FinancePlaceholderPage("
new = placeholder + "\n@Composable\nprivate fun FinancePlaceholderPage("

if old in content:
    content = content.replace(old, new)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Done: added CalendarPlaceholderPage")
else:
    print("ERROR: pattern not found")
    # Search for FinancePlaceholderPage
    idx = content.find("FinancePlaceholderPage")
    print(f"FinancePlaceholderPage found at index {idx}")
    if idx >= 0:
        print(content[idx-50:idx+50])
