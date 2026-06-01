package github.leavesczy.compose_chat.open.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.repository.OnCallStreamEvent
import github.leavesczy.compose_chat.open.repository.OpenOnCallRepository
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun OpenOnCallPage(
    modifier: Modifier = Modifier,
    repository: OpenOnCallRepository = remember { OpenOnCallRepository() }
) {
    val messages = remember {
        mutableStateListOf<OnCallMessageUi>(
            OnCallMessageUi.Assistant(
                id = "welcome",
                content = "你好，我是 AI oncall。你可以把 TabManifest、错误码、日志或接入疑问发给我，我会围绕协议给出建议。",
                streaming = false,
                failedMessage = null
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var currentIntent by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun sendMessage(message: String) {
        val content = message.trim()
        if (content.isBlank() || sending) {
            return
        }
        input = ""
        sending = true
        messages += OnCallMessageUi.User(id = UUID.randomUUID().toString(), content = content)
        val assistantId = UUID.randomUUID().toString()
        messages += OnCallMessageUi.Assistant(
            id = assistantId,
            content = "",
            streaming = true,
            failedMessage = null
        )
        scope.launch {
            repository.stream(message = content).collect { event ->
                val assistantIndex = messages.indexOfFirst { it.id == assistantId }
                if (assistantIndex == -1) {
                    return@collect
                }
                when (event) {
                    is OnCallStreamEvent.Delta -> {
                        val old = messages[assistantIndex] as OnCallMessageUi.Assistant
                        messages[assistantIndex] = old.copy(content = old.content + event.text)
                    }

                    is OnCallStreamEvent.Tool -> {
                        messages += OnCallMessageUi.Tool(
                            id = UUID.randomUUID().toString(),
                            tool = event.tool
                        )
                    }

                    is OnCallStreamEvent.Done -> {
                        val old = messages[assistantIndex] as OnCallMessageUi.Assistant
                        messages[assistantIndex] = old.copy(streaming = false)
                        sending = false
                    }

                    is OnCallStreamEvent.Intent -> {
                        currentIntent = event.intent
                    }


                    is OnCallStreamEvent.Error -> {
                        val old = messages[assistantIndex] as OnCallMessageUi.Assistant
                        messages[assistantIndex] = old.copy(
                            streaming = false,
                            failedMessage = "${event.code}：${event.message}"
                        )
                        sending = false
                    }

                    is OnCallStreamEvent.Unknown -> {
                        // 未知事件暂不打断主流程。服务端扩展新事件后可在这里补充新的展示卡片。
                    }
                }
            }
            sending = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(index = messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(color = 0xFFF6F8FB))
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        OnCallHeader()
        LazyColumn(
            modifier = Modifier
                .weight(weight = 1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
            item {
                CapabilityPanel()
            }
            item {
                QuickQuestionRow(
                    enabled = !sending,
                    onClick = ::sendMessage
                )
            }
            items(items = messages, key = { it.id }) { message ->
                when (message) {
                    is OnCallMessageUi.User -> UserMessageBubble(message = message)
                    is OnCallMessageUi.Assistant -> AssistantMessageBubble(
                        message = message,
                        onRetry = {
                            val lastUser = messages.lastOrNull { item ->
                                item is OnCallMessageUi.User
                            } as? OnCallMessageUi.User
                            if (lastUser != null) {
                                sendMessage(lastUser.content)
                            }
                        }
                    )

                    is OnCallMessageUi.Tool -> ToolMessageCard(message = message)

                    is OnCallMessageUi.Intent -> { /* intent handled via currentIntent state */ }
                }
            }
        }
        OnCallInputBar(
            value = input,
            sending = sending,
            onValueChange = { input = it },
            onSend = { sendMessage(input) }
        )
    }
}

@Composable
private fun OnCallHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 14.dp))
                .background(color = Color(color = 0xFF2563EB))
                .padding(all = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            Text(
                text = "AI oncall",
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = "协议问答 · 配置生成 · 错误诊断",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = Color(color = 0xFF6B7280)
            )
        }
        Text(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color(color = 0xFFEAF7EF))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            text = "SSE 可用",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF15803D)
        )
    }
}

@Composable
private fun CapabilityPanel() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        CapabilityCard(
            modifier = Modifier.weight(weight = 1f),
            icon = Icons.Rounded.Description,
            title = "协议问答"
        )
        CapabilityCard(
            modifier = Modifier.weight(weight = 1f),
            icon = Icons.Rounded.Build,
            title = "配置诊断"
        )
        CapabilityCard(
            modifier = Modifier.weight(weight = 1f),
            icon = Icons.Rounded.ErrorOutline,
            title = "错误定位"
        )
    }
}

@Composable
private fun CapabilityCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 14.dp))
            .background(color = Color.White)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(size = 22.dp),
            imageVector = icon,
            contentDescription = null,
            tint = Color(color = 0xFF2563EB)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
    }
}

@Composable
private fun QuickQuestionRow(
    enabled: Boolean,
    onClick: (String) -> Unit
) {
    val questions = listOf(
        "如何接入一个业务 Tab？",
        "TabManifest 必填字段有哪些？",
        "为什么会版本不兼容？"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        questions.forEach { question ->
            Text(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = Color.White)
                    .clickable(enabled = enabled) {
                        onClick(question)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                text = question,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = if (enabled) Color(color = 0xFF2563EB) else Color(color = 0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun UserMessageBubble(message: OnCallMessageUi.User) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 292.dp)
                .clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 6.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(color = Color(color = 0xFF2563EB))
                .padding(horizontal = 13.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    message: OnCallMessageUi.Assistant,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(size = 28.dp)
                .clip(shape = CircleShape)
                .background(color = Color(color = 0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 17.dp),
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = Color(color = 0xFF2563EB)
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .widthIn(max = 310.dp)
                .clip(shape = RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(color = Color.White)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Text(
                text = message.content.ifBlank { if (message.streaming) "正在分析协议上下文…" else "暂无回复" },
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = Color(color = 0xFF111827)
            )
            if (message.content.contains("```")) {
                Text(
                    text = "检测到代码块：后续会升级为独立代码块渲染。",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(color = 0xFF6B7280)
                )
            }
            if (message.streaming) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size = 14.dp),
                        strokeWidth = 2.dp,
                        color = Color(color = 0xFF2563EB)
                    )
                    Text(
                        text = "正在生成",
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = Color(color = 0xFF6B7280)
                    )
                }
            }
            if (message.failedMessage != null) {
                Text(
                    text = message.failedMessage,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(color = 0xFFB91C1C)
                )
                Text(
                    modifier = Modifier.clickable(onClick = onRetry),
                    text = "重试上一条问题",
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(color = 0xFF2563EB)
                )
            }
        }
    }
}

@Composable
private fun ToolMessageCard(message: OnCallMessageUi.Tool) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 14.dp))
            .background(color = Color(color = 0xFFFFF7ED))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Text(
            text = "工具调用：${message.tool.name}",
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFFC2410C)
        )
        Text(
            text = "${message.tool.status} · ${message.tool.summary}",
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = Color(color = 0xFF7C2D12)
        )
    }
}

@Composable
private fun OnCallInputBar(
    value: String,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 18.dp))
            .background(color = Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(weight = 1f),
            value = value,
            onValueChange = onValueChange,
            minLines = 1,
            maxLines = 4,
            placeholder = {
                Text(text = "输入协议问题、错误码或日志")
            },
            shape = RoundedCornerShape(size = 14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = Color(color = 0xFF2563EB),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(color = 0xFFF3F6FA),
                unfocusedContainerColor = Color(color = 0xFFF3F6FA)
            )
        )
        Box(
            modifier = Modifier
                .size(size = 48.dp)
                .clip(shape = CircleShape)
                .background(
                    color = if (sending) {
                        Color(color = 0xFF111827)
                    } else if (value.isBlank()) {
                        Color(color = 0xFFE5E7EB)
                    } else {
                        Color(color = 0xFF2563EB)
                    }
                )
                .clickable(enabled = !sending && value.isNotBlank(), onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (sending) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                contentDescription = if (sending) "生成中" else "发送",
                tint = if (value.isBlank() && !sending) Color(color = 0xFF9CA3AF) else Color.White
            )
        }
    }
}

private sealed class OnCallMessageUi {

    abstract val id: String

    data class User(
        override val id: String,
        val content: String
    ) : OnCallMessageUi()

    data class Assistant(
        override val id: String,
        val content: String,
        val streaming: Boolean,
        val failedMessage: String?
    ) : OnCallMessageUi()

    data class Tool(
        override val id: String,
        val tool: OnCallToolEvent
    ) : OnCallMessageUi()

    data class Intent(
        override val id: String,
        val intent: String
    ) : OnCallMessageUi()

}
