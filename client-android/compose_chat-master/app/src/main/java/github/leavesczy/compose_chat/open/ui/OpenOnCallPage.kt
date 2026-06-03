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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.open.model.OnCallSessionDto
import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OnCallStreamEvent
import github.leavesczy.compose_chat.open.repository.OpenOnCallRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun OpenOnCallHomePage(
    modifier: Modifier = Modifier,
    onOpenChat: (
        sessionId: String?,
        sessionTitle: String?,
        initialPrompt: String?,
        forceNewSession: Boolean
    ) -> Unit,
    repository: OpenOnCallRepository = remember { OpenOnCallRepository() }
) {
    var loading by remember { mutableStateOf(true) }
    var sessions by remember { mutableStateOf<List<OnCallSessionDto>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteSession by remember { mutableStateOf<OnCallSessionDto?>(null) }
    var deletingSessionId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadSessions() {
        loading = true
        scope.launch {
            when (val result = repository.sessions()) {
                is OpenApiResult.Success -> {
                    sessions = result.data.sortedByDescending { session ->
                        session.updatedAt ?: session.createdAt
                    }
                    errorMessage = null
                }

                is OpenApiResult.Failed -> {
                    errorMessage = "会话列表加载失败：${result.message}"
                }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadSessions()
    }

    pendingDeleteSession?.let { session ->
        AlertDialog(
            onDismissRequest = {
                if (deletingSessionId == null) {
                    pendingDeleteSession = null
                }
            },
            title = {
                Text(text = "删除会话")
            },
            text = {
                Text(text = "确定删除“${session.title.ifBlank { "未命名咨询" }}”吗？删除后无法在最近会话中继续查看。")
            },
            confirmButton = {
                TextButton(
                    enabled = deletingSessionId == null,
                    onClick = {
                        deletingSessionId = session.sessionId
                        scope.launch {
                            when (val result = repository.deleteSession(sessionId = session.sessionId)) {
                                is OpenApiResult.Success -> {
                                    pendingDeleteSession = null
                                    deletingSessionId = null
                                    loadSessions()
                                }

                                is OpenApiResult.Failed -> {
                                    errorMessage = "删除会话失败：${result.message}"
                                    pendingDeleteSession = null
                                    deletingSessionId = null
                                }
                            }
                        }
                    }
                ) {
                    Text(text = if (deletingSessionId == session.sessionId) "删除中" else "删除")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = deletingSessionId == null,
                    onClick = {
                        pendingDeleteSession = null
                    }
                ) {
                    Text(text = "取消")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(color = 0xFFF6F8FB))
            .statusBarsPadding()
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        OnCallHomeHeader(onRefresh = ::loadSessions)
        PrimaryOnCallAction(
            title = "新建咨询",
            body = "从一个空白会话开始排查协议、接口或配置问题",
            onClick = {
                onOpenChat(null, null, null, true)
            }
        )
        SectionTitle(text = "快捷能力")
        CapabilityPanel(
            onClick = { preset ->
                onOpenChat(null, preset.title, preset.prompt, true)
            }
        )
        SectionTitle(text = "最近会话")
        when {
            loading -> {
                HomeInfoCard(title = "正在加载", body = "正在同步最近的 AI 咨询记录…")
            }

            errorMessage != null -> {
                HomeInfoCard(title = "会话加载失败", body = errorMessage.orEmpty())
            }

            sessions.isEmpty() -> {
                HomeInfoCard(title = "暂无会话", body = "可以先新建一次咨询，后续这里会展示最近的聊天记录。")
            }

            else -> {
                sessions.take(n = 6).forEach { session ->
                    SessionRow(
                        session = session,
                        onClick = {
                            onOpenChat(session.sessionId, session.title, null, false)
                        },
                        onDelete = {
                            pendingDeleteSession = session
                        }
                    )
                }
            }
        }
        SectionTitle(text = "建议问题")
        QuickQuestionRow(
            enabled = true,
            onClick = { question ->
                onOpenChat(null, "快速咨询", question, true)
            }
        )
    }
}

@Composable
fun OpenOnCallPage(
    modifier: Modifier = Modifier,
    onBackToOnCallHome: (() -> Unit)? = null,
    initialSessionId: String? = null,
    initialSessionTitle: String? = null,
    initialPrompt: String? = null,
    forceNewSession: Boolean = false,
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
    var sessionId by remember { mutableStateOf(initialSessionId) }
    var sessionTitle by remember { mutableStateOf(initialSessionTitle ?: "临时会话") }
    var sessionMode by remember { mutableStateOf("正在创建会话") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun isNearConversationEnd(): Boolean {
        if (messages.isEmpty()) {
            return true
        }
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return true
        return lastVisibleIndex >= messages.lastIndex - 1
    }

    fun scrollToConversationEnd(animated: Boolean = true) {
        if (messages.isEmpty()) {
            return
        }
        scope.launch {
            if (animated) {
                listState.animateScrollToItem(index = messages.lastIndex)
            } else {
                listState.scrollToItem(index = messages.lastIndex)
            }
        }
    }

    suspend fun loadHistory(activeSessionId: String) {
        when (val historyResult = repository.messages(sessionId = activeSessionId)) {
            is OpenApiResult.Success -> {
                val historyMessages = historyResult.data.filter { message ->
                    message.content.isNotBlank()
                }
                if (historyMessages.isNotEmpty()) {
                    messages.clear()
                    historyMessages.forEach { message ->
                        messages += if (message.role == "user") {
                            OnCallMessageUi.User(
                                id = message.messageId,
                                content = message.content
                            )
                        } else {
                            OnCallMessageUi.Assistant(
                                id = message.messageId,
                                content = message.content,
                                streaming = false,
                                failedMessage = null
                            )
                        }
                    }
                }
            }

            is OpenApiResult.Failed -> {
                sessionMode = "多轮会话，历史加载失败"
            }
        }
    }

    fun sendMessage(message: String) {
        val content = message.trim()
        if (content.isBlank() || sending) {
            return
        }
        input = ""
        sending = true
        messages.removeAll { item -> item.id.startsWith(prefix = "welcome") }
        messages += OnCallMessageUi.User(id = UUID.randomUUID().toString(), content = content)
        val assistantId = UUID.randomUUID().toString()
        messages += OnCallMessageUi.Assistant(
            id = assistantId,
            content = "",
            streaming = true,
            failedMessage = null
        )
        scope.launch {
            val activeSessionId = sessionId
            val streamFlow = if (activeSessionId == null) {
                sessionMode = "旧版单轮 SSE"
                repository.stream(message = content)
            } else {
                when (val postResult = repository.postMessage(sessionId = activeSessionId, content = content)) {
                    is OpenApiResult.Success -> {
                        sessionMode = "多轮会话"
                        repository.stream(sessionId = activeSessionId, messageId = postResult.data.messageId)
                    }

                    is OpenApiResult.Failed -> {
                        val assistantIndex = messages.indexOfFirst { it.id == assistantId }
                        if (assistantIndex != -1) {
                            val old = messages[assistantIndex] as OnCallMessageUi.Assistant
                            messages[assistantIndex] = old.copy(
                                streaming = false,
                                failedMessage = "${postResult.code}：${postResult.message}"
                            )
                        }
                        sending = false
                        return@launch
                    }
                }
            }
            streamFlow.collect { event ->
                handleStreamEvent(
                    messages = messages,
                    assistantId = assistantId,
                    event = event,
                    onDone = { sending = false }
                )
            }
            sending = false
        }
    }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            input = initialPrompt
        }
    }

    LaunchedEffect(initialSessionId, forceNewSession) {
        if (initialSessionId != null && !forceNewSession) {
            sessionId = initialSessionId
            sessionTitle = initialSessionTitle?.ifBlank { "Tab 接入咨询" } ?: "Tab 接入咨询"
            sessionMode = "多轮会话"
            loadHistory(activeSessionId = initialSessionId)
            return@LaunchedEffect
        }
        if (forceNewSession) {
            when (val result = repository.createSession(title = initialSessionTitle ?: "Tab 接入咨询")) {
                is OpenApiResult.Success -> {
                    sessionId = result.data.sessionId
                    sessionTitle = result.data.title.ifBlank { initialSessionTitle ?: "Tab 接入咨询" }
                    sessionMode = "多轮会话"
                    messages.clear()
                    messages += OnCallMessageUi.Assistant(
                        id = "welcome-${result.data.sessionId}",
                        content = "新会话已创建，可以开始提问。",
                        streaming = false,
                        failedMessage = null
                    )
                }

                is OpenApiResult.Failed -> {
                    sessionMode = "旧版单轮 SSE"
                }
            }
            return@LaunchedEffect
        }
        when (val sessionsResult = repository.sessions()) {
            is OpenApiResult.Success -> {
                val latestSession = sessionsResult.data.maxByOrNull { session ->
                    session.updatedAt ?: session.createdAt
                }
                if (latestSession != null) {
                    sessionId = latestSession.sessionId
                    sessionTitle = latestSession.title.ifBlank { "Tab 接入咨询" }
                    sessionMode = "多轮会话"
                    loadHistory(activeSessionId = latestSession.sessionId)
                    return@LaunchedEffect
                }
            }

            is OpenApiResult.Failed -> {
                // 会话列表失败时继续尝试新建会话；新建也失败才降级到旧 SSE。
            }
        }
        when (val result = repository.createSession(title = "Tab 接入咨询")) {
            is OpenApiResult.Success -> {
                sessionId = result.data.sessionId
                sessionTitle = result.data.title.ifBlank { "Tab 接入咨询" }
                sessionMode = "多轮会话"
            }

            is OpenApiResult.Failed -> {
                sessionMode = "旧版单轮 SSE"
            }
        }
    }

    fun resetSession() {
        if (sending) {
            return
        }
        scope.launch {
            sessionMode = "正在创建会话"
            when (val result = repository.createSession(title = "Tab 接入咨询")) {
                is OpenApiResult.Success -> {
                    sessionId = result.data.sessionId
                    sessionTitle = result.data.title.ifBlank { "Tab 接入咨询" }
                    sessionMode = "多轮会话"
                    messages.clear()
                    messages += OnCallMessageUi.Assistant(
                        id = "welcome-${result.data.sessionId}",
                        content = "新会话已创建，可以开始提问。",
                        streaming = false,
                        failedMessage = null
                    )
                }

                is OpenApiResult.Failed -> {
                    sessionMode = "旧版单轮 SSE"
                    messages += OnCallMessageUi.Assistant(
                        id = UUID.randomUUID().toString(),
                        content = "新会话创建失败，已切回旧版单轮 SSE：${result.message}",
                        streaming = false,
                        failedMessage = null
                    )
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        val latest = messages.lastOrNull()
        if (
            latest is OnCallMessageUi.User ||
            latest is OnCallMessageUi.Assistant && latest.streaming ||
            latest is OnCallMessageUi.Tool
        ) {
            scrollToConversationEnd()
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
        OnCallHeader(
            onBackToOnCallHome = onBackToOnCallHome,
            sessionTitle = sessionTitle,
            onNewSession = ::resetSession
        )
        LazyColumn(
            modifier = Modifier
                .weight(weight = 1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
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
                }
            }
        }
        OnCallInputBar(
            value = input,
            sending = sending,
            onValueChange = { input = it },
            onFocusInput = {
                if (isNearConversationEnd()) {
                    scope.launch {
                        delay(timeMillis = 180)
                        scrollToConversationEnd()
                    }
                }
            },
            onSend = { sendMessage(input) }
        )
    }
}

@Composable
private fun OnCallHeader(
    onBackToOnCallHome: (() -> Unit)?,
    sessionTitle: String,
    onNewSession: () -> Unit
) {
    val compactTitle = sessionTitle.toCompactSessionTitle()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackToOnCallHome != null) {
            Box(
                modifier = Modifier
                    .size(size = 38.dp)
                    .clip(shape = CircleShape)
                    .background(color = Color.White)
                    .clickable(onClick = onBackToOnCallHome),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(size = 20.dp),
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回 AI 助手",
                    tint = Color(color = 0xFF2563EB)
                )
            }
        }
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = compactTitle,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color.White)
                .clickable(onClick = onNewSession)
                .padding(horizontal = 11.dp, vertical = 7.dp),
            text = "新会话",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF2563EB)
        )
    }
}

@Composable
private fun OnCallHomeHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                text = "AI助手",
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = "最近会话、快捷能力与问题诊断。",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Color(color = 0xFF6B7280)
            )
        }
        Box(
            modifier = Modifier
                .size(size = 40.dp)
                .clip(shape = RoundedCornerShape(size = 12.dp))
                .background(color = Color.White)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 21.dp),
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "刷新会话",
                tint = Color(color = 0xFF2563EB)
            )
        }
    }
}

@Composable
private fun PrimaryOnCallAction(
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 18.dp))
            .background(color = Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(size = 42.dp)
                    .clip(shape = CircleShape)
                    .background(color = Color(color = 0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(color = 0xFF111827)
                )
                Text(
                    text = body,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = Color(color = 0xFF6B7280)
                )
            }
            Text(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = Color(color = 0xFFEFF6FF))
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                text = "开始",
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF2563EB)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 12.dp))
                .background(color = Color(color = 0xFFF3F6FA))
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Text(
                text = "输入 Tab 配置、接口错误或接入问题",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Color(color = 0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color(color = 0xFF111827)
    )
}

@Composable
private fun CapabilityPanel(onClick: (OnCallPromptPreset) -> Unit) {
    val presets = remember {
        listOf(
            OnCallPromptPreset(
                title = "协议问答",
                icon = Icons.Rounded.Description,
                prompt = "请帮我解释开放式 Tab 协议里 TabManifest 的关键字段和必填规则。"
            ),
            OnCallPromptPreset(
                title = "配置诊断",
                icon = Icons.Rounded.Build,
                prompt = "请帮我检查这个 TabManifest 配置是否有问题：\n"
            ),
            OnCallPromptPreset(
                title = "错误定位",
                icon = Icons.Rounded.ErrorOutline,
                prompt = "请帮我根据下面的错误码或日志定位问题：\n"
            )
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        presets.forEach { preset ->
            CapabilityCard(
                modifier = Modifier.weight(weight = 1f),
                icon = preset.icon,
                title = preset.title,
                onClick = { onClick(preset) }
            )
        }
    }
}

@Composable
private fun CapabilityCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 14.dp))
            .background(color = Color.White)
            .clickable(onClick = onClick)
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
private fun SessionRow(
    session: OnCallSessionDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val title = session.title.ifBlank { "未命名咨询" }
    val meta = listOfNotNull(
        session.messageCount?.let { "${it} 条" },
        formatSessionTime(value = session.updatedAt ?: session.createdAt)
    ).joinToString(separator = " · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 10.dp))
            .background(color = Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(size = 32.dp)
                .clip(shape = CircleShape)
                .background(color = Color(color = 0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(n = 1),
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF2563EB)
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 5.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = meta.ifBlank { "暂无更新时间" },
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = Color(color = 0xFF6B7280)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                modifier = Modifier.size(size = 20.dp),
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "删除会话",
                tint = Color(color = 0xFF6B7280)
            )
        }
    }
}

@Composable
private fun HomeInfoCard(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = Color.White)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = Color(color = 0xFF6B7280)
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
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        questions.forEach { question ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = Color.White)
                    .clickable(enabled = enabled) {
                        onClick(question)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(size = 16.dp),
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = if (enabled) Color(color = 0xFF2563EB) else Color(color = 0xFF9CA3AF)
                )
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = question,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = if (enabled) Color(color = 0xFF2563EB) else Color(color = 0xFF9CA3AF)
                )
            }
        }
    }
}

private data class OnCallPromptPreset(
    val title: String,
    val icon: ImageVector,
    val prompt: String
)

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
            AssistantMarkdownText(
                content = message.content,
                streaming = message.streaming
            )
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
    onFocusInput: () -> Unit,
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
            modifier = Modifier
                .weight(weight = 1f)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onFocusInput()
                    }
                },
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

private fun handleStreamEvent(
    messages: MutableList<OnCallMessageUi>,
    assistantId: String,
    event: OnCallStreamEvent,
    onDone: () -> Unit
) {
    val assistantIndex = messages.indexOfFirst { it.id == assistantId }
    if (assistantIndex == -1) {
        return
    }
    when (event) {
        is OnCallStreamEvent.Delta -> {
            val old = messages[assistantIndex] as OnCallMessageUi.Assistant
            messages[assistantIndex] = old.copy(content = old.content + event.text)
        }

        is OnCallStreamEvent.Tool -> {
            if (event.tool.status.isVisibleToolStatus()) {
                messages += OnCallMessageUi.Tool(
                    id = UUID.randomUUID().toString(),
                    tool = event.tool
                )
            }
        }

        is OnCallStreamEvent.Done -> {
            val old = messages[assistantIndex] as OnCallMessageUi.Assistant
            messages[assistantIndex] = old.copy(
                content = old.content.collapseDuplicatedReply(),
                streaming = false
            )
            removeAdjacentDuplicatedAssistant(messages = messages, assistantId = assistantId)
            onDone()
        }

        is OnCallStreamEvent.Error -> {
            val old = messages[assistantIndex] as OnCallMessageUi.Assistant
            messages[assistantIndex] = old.copy(
                streaming = false,
                failedMessage = "${event.code}：${event.message}"
            )
            onDone()
        }

        is OnCallStreamEvent.Unknown -> {
            // 未知事件暂不打断主流程。服务端扩展新事件后可在这里补充新的展示卡片。
        }
    }
}

private fun removeAdjacentDuplicatedAssistant(
    messages: MutableList<OnCallMessageUi>,
    assistantId: String
) {
    val assistantIndex = messages.indexOfFirst { item -> item.id == assistantId }
    if (assistantIndex <= 0) {
        return
    }
    val current = messages[assistantIndex] as? OnCallMessageUi.Assistant ?: return
    val previousAssistantIndex = messages
        .take(n = assistantIndex)
        .indexOfLast { item -> item is OnCallMessageUi.Assistant }
    if (previousAssistantIndex == -1) {
        return
    }
    val hasUserBetween = messages
        .subList(fromIndex = previousAssistantIndex + 1, toIndex = assistantIndex)
        .any { item -> item is OnCallMessageUi.User }
    if (hasUserBetween) {
        return
    }
    val previous = messages[previousAssistantIndex] as? OnCallMessageUi.Assistant ?: return
    if (
        !previous.streaming &&
        previous.failedMessage == null &&
        current.failedMessage == null &&
        previous.content.normalizedReplyText() == current.content.normalizedReplyText()
    ) {
        messages.removeAt(index = assistantIndex)
    }
}

private fun String.collapseDuplicatedReply(): String {
    val value = trim()
    if (value.length < 16 || value.length % 2 != 0) {
        return this
    }
    val half = value.length / 2
    val first = value.substring(startIndex = 0, endIndex = half).trim()
    val second = value.substring(startIndex = half).trim()
    return if (first == second) first else this
}

private fun String.normalizedReplyText(): String {
    return trim().replace(regex = "\\s+".toRegex(), replacement = " ")
}

@Composable
private fun AssistantMarkdownText(
    content: String,
    streaming: Boolean
) {
    val fallback = if (streaming) "正在分析协议上下文..." else "暂无回复"
    val blocks = remember(content, streaming) {
        parseAssistantMarkdown(content.ifBlank { fallback })
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(space = 7.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        fontSize = if (block.level <= 2) 17.sp else 16.sp,
                        lineHeight = if (block.level <= 2) 22.sp else 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(color = 0xFF111827)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = Color(color = 0xFF111827)
                    )
                }

                is MarkdownBlock.ListBlock -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
                    ) {
                        block.items.forEach { item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(space = 7.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    color = Color(color = 0xFF2563EB)
                                )
                                Text(
                                    modifier = Modifier.weight(weight = 1f),
                                    text = parseInlineMarkdown(item),
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    color = Color(color = 0xFF111827)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Code -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = RoundedCornerShape(size = 10.dp))
                            .background(color = Color(color = 0xFFF3F4F6))
                            .horizontalScroll(state = rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = block.text.ifBlank { " " },
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(color = 0xFF111827)
                        )
                    }
                }

                is MarkdownBlock.Table -> {
                    MarkdownTable(block = block)
                }
            }
        }
    }
}

private fun parseAssistantMarkdown(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val listItems = mutableListOf<String>()
    val tableRows = mutableListOf<List<String>>()
    val codeLines = mutableListOf<String>()
    var inCode = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(text = paragraph.joinToString(separator = "\n").trim())
            paragraph.clear()
        }
    }

    fun flushList() {
        if (listItems.isNotEmpty()) {
            blocks += MarkdownBlock.ListBlock(items = listItems.toList())
            listItems.clear()
        }
    }

    fun flushTable() {
        if (tableRows.isNotEmpty()) {
            val header = tableRows.first()
            val body = tableRows.drop(n = 1)
            blocks += MarkdownBlock.Table(header = header, rows = body)
            tableRows.clear()
        }
    }

    content.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                blocks += MarkdownBlock.Code(text = codeLines.joinToString(separator = "\n"))
                codeLines.clear()
            } else {
                flushParagraph()
                flushList()
                flushTable()
            }
            inCode = !inCode
            return@forEach
        }
        if (inCode) {
            codeLines += rawLine
            return@forEach
        }
        if (trimmed.isBlank()) {
            flushParagraph()
            flushList()
            flushTable()
            return@forEach
        }
        val headingLevel = trimmed.takeWhile { it == '#' }.length
        if (headingLevel in 1..6 && trimmed.drop(headingLevel).startsWith(" ")) {
            flushParagraph()
            flushList()
            flushTable()
            blocks += MarkdownBlock.Heading(
                level = headingLevel,
                text = trimmed.drop(headingLevel).trim()
            )
            return@forEach
        }
        val tableRow = parseMarkdownTableRow(trimmed)
        if (tableRow != null) {
            flushParagraph()
            flushList()
            if (!tableRow.isMarkdownDividerRow()) {
                tableRows += tableRow
            }
            return@forEach
        }
        val unorderedItem = when {
            trimmed.startsWith("- ") -> trimmed.drop(2).trim()
            trimmed.startsWith("* ") -> trimmed.drop(2).trim()
            else -> null
        }
        val orderedItem = trimmed.replaceFirst(regex = """^\d+[.)]\s+""".toRegex(), replacement = "")
            .takeIf { it != trimmed }
        val listItem = unorderedItem ?: orderedItem
        if (listItem != null) {
            flushParagraph()
            flushTable()
            listItems += listItem
            return@forEach
        }
        flushList()
        flushTable()
        paragraph += line
    }
    if (inCode && codeLines.isNotEmpty()) {
        blocks += MarkdownBlock.Code(text = codeLines.joinToString(separator = "\n"))
    }
    flushParagraph()
    flushList()
    flushTable()
    return blocks.ifEmpty { listOf(MarkdownBlock.Paragraph(text = content)) }
}

@Composable
private fun MarkdownTable(block: MarkdownBlock.Table) {
    val columnCount = maxOf(
        block.header.size,
        block.rows.maxOfOrNull { it.size } ?: 0
    ).coerceAtLeast(minimumValue = 1)
    val cellWidth = 132.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(state = rememberScrollState())
            .clip(shape = RoundedCornerShape(size = 10.dp))
            .background(color = Color(color = 0xFFF8FAFC))
    ) {
        TableRow(
            cells = block.header.normalizeCellCount(columnCount),
            cellWidth = cellWidth,
            backgroundColor = Color(color = 0xFFEFF6FF),
            fontWeight = FontWeight.Bold
        )
        block.rows.forEachIndexed { index, row ->
            TableRow(
                cells = row.normalizeCellCount(columnCount),
                cellWidth = cellWidth,
                backgroundColor = if (index % 2 == 0) {
                    Color.White
                } else {
                    Color(color = 0xFFF8FAFC)
                },
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    cellWidth: androidx.compose.ui.unit.Dp,
    backgroundColor: Color,
    fontWeight: FontWeight
) {
    Row(
        modifier = Modifier.background(color = backgroundColor)
    ) {
        cells.forEach { cell ->
            Text(
                modifier = Modifier
                    .width(width = cellWidth)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                text = parseInlineMarkdown(cell),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = fontWeight,
                color = Color(color = 0xFF111827)
            )
        }
    }
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    if (!text.contains("**") && !text.contains("`")) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", startIndex = index) -> {
                    val end = text.indexOf(string = "**", startIndex = index + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(startIndex = index + 2, endIndex = end))
                        }
                        index = end + 2
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                text[index] == '`' -> {
                    val end = text.indexOf(char = '`', startIndex = index + 1)
                    if (end != -1) {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = Color(color = 0xFF1D4ED8),
                                background = Color(color = 0xFFEFF6FF)
                            )
                        ) {
                            append(text.substring(startIndex = index + 1, endIndex = end))
                        }
                        index = end + 1
                    } else {
                        append(text[index])
                        index += 1
                    }
                }

                else -> {
                    append(text[index])
                    index += 1
                }
            }
        }
    }
}

private fun parseMarkdownTableRow(line: String): List<String>? {
    if (!line.contains("|")) {
        return null
    }
    val trimmed = line.trim()
    if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
        return null
    }
    val cells = trimmed
        .trim('|')
        .split("|")
        .map { it.trim() }
    return cells.takeIf { row -> row.size >= 2 && row.any { it.isNotBlank() } }
}

private fun List<String>.isMarkdownDividerRow(): Boolean {
    return isNotEmpty() && all { cell ->
        cell.isNotBlank() && cell.all { char ->
            char == '-' || char == ':' || char == ' '
        }
    }
}

private fun List<String>.normalizeCellCount(count: Int): List<String> {
    return if (size >= count) {
        take(count)
    } else {
        this + List(size = count - size) { "" }
    }
}

private fun formatSessionTime(value: String?): String {
    if (value.isNullOrBlank()) {
        return "暂无更新时间"
    }
    return runCatching {
        val sessionTime = OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
        val now = java.time.LocalDateTime.now()
        val duration = Duration.between(sessionTime, now)
        when {
            duration.toMinutes() < 1 -> "刚刚"
            duration.toHours() < 1 -> "${duration.toMinutes()} 分钟前"
            sessionTime.toLocalDate() == LocalDate.now() -> {
                "今天 ${sessionTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }

            sessionTime.toLocalDate() == LocalDate.now().minusDays(1) -> {
                "昨天 ${sessionTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }

            sessionTime.year == now.year -> {
                sessionTime.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))
            }

            else -> {
                sessionTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
            }
        }
    }.getOrElse {
        value.replace(oldValue = "T", newValue = " ").substringBefore(delimiter = "+")
    }
}

private fun String.toCompactSessionTitle(): String {
    val cleanTitle = trim()
        .ifBlank { "新咨询" }
        .replace(oldValue = "Tab 接入咨询", newValue = "Tab接入")
        .replace(oldValue = "快速咨询", newValue = "快速问答")
        .replace(oldValue = "临时会话", newValue = "新咨询")
        .replace(oldValue = "未命名咨询", newValue = "新咨询")
    return cleanTitle.take(n = 5)
}

private fun String.isVisibleToolStatus(): Boolean {
    return trim().lowercase() in setOf(
        "completed",
        "complete",
        "success",
        "succeeded",
        "error",
        "failed",
        "failure"
    )
}

private sealed class MarkdownBlock {

    data class Heading(
        val level: Int,
        val text: String
    ) : MarkdownBlock()

    data class Paragraph(
        val text: String
    ) : MarkdownBlock()

    data class ListBlock(
        val items: List<String>
    ) : MarkdownBlock()

    data class Table(
        val header: List<String>,
        val rows: List<List<String>>
    ) : MarkdownBlock()

    data class Code(
        val text: String
    ) : MarkdownBlock()

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

}
