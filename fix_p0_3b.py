import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenOnCallPage.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: Add Sources to OnCallMessageUi sealed class
old_sealed = """    data class Tool(
        override val id: String,
        val tool: OnCallToolEvent
    ) : OnCallMessageUi()

}"""

new_sealed = """    data class Tool(
        override val id: String,
        val tool: OnCallToolEvent
    ) : OnCallMessageUi()

    data class SourceCard(
        override val id: String,
        val file: String,
        val relevance: Double,
        val snippet: String
    ) : OnCallMessageUi()

}"""

content = content.replace(old_sealed, new_sealed)

# Fix 2: Handle SourcesEvent in handleStreamEvent
old_sources_handler = """        is OnCallStreamEvent.Sources -> {
            // Sources marks knowledge base documents, shown in Assistant message
        }"""

new_sources_handler = """        is OnCallStreamEvent.Sources -> {
            event.items.forEach { item ->
                messages += OnCallMessageUi.SourceCard(
                    id = UUID.randomUUID().toString(),
                    file = item.file,
                    relevance = item.relevance,
                    snippet = item.snippet
                )
            }
        }"""

content = content.replace(old_sources_handler, new_sources_handler)

# Fix 3: Add IntentEvent display
old_intent_handler = """        is OnCallStreamEvent.Intent -> {
            // Intent only marks user intent type, no UI needed
        }"""

new_intent_handler = """        is OnCallStreamEvent.Intent -> {
            messages += OnCallMessageUi.Tool(
                id = UUID.randomUUID().toString(),
                tool = OnCallToolEvent(
                    name = event.intent,
                    status = "done",
                    summary = when (event.intent) {
                        "PROTOCOL_QA" -> "协议问答"
                        "ERROR_DIAGNOSIS" -> "错误诊断"
                        "CODE_GENERATION" -> "代码生成"
                        else -> "通用助手"
                    }
                )
            )
        }"""

content = content.replace(old_intent_handler, new_intent_handler)

# Fix 4: Add SourcesMessageCard rendering in the when block
old_when = """                        is OnCallMessageUi.Tool -> ToolMessageCard(message = message)"""

new_when = """                        is OnCallMessageUi.Tool -> ToolMessageCard(message = message)
                        is OnCallMessageUi.SourceCard -> SourceMessageCard(message = message)"""

content = content.replace(old_when, new_when)

# Fix 5: Add SourceMessageCard composable before ToolMessageCard
old_toolcard = """private fun ToolMessageCard(message: OnCallMessageUi.Tool) {"""

new_toolcard = """@Composable
private fun SourceMessageCard(message: OnCallMessageUi.SourceCard) {
    val relevancePct = (message.relevance * 100).toInt()
    Row(
        modifier = Modifier
            .padding(start = 52.dp, top = 2.dp, bottom = 2.dp)
            .clip(shape = RoundedCornerShape(size = 6.dp))
            .background(color = Color(color = 0xFFF0FDF4))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp)
    ) {
        Icon(
            modifier = Modifier.size(size = 14.dp),
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = Color(color = 0xFF16A34A)
        )
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = message.file,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(color = 0xFF166534),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (message.relevance > 0) {
                Text(
                    text = "匹配度 ${relevancePct}%",
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = Color(color = 0xFF4ADE80)
                )
            }
        }
    }
}

private fun ToolMessageCard(message: OnCallMessageUi.Tool) {"""

content = content.replace(old_toolcard, new_toolcard)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("OpenOnCallPage done")
