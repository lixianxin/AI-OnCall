package github.leavesczy.compose_chat.ui.oncall.logic

import github.leavesczy.compose_chat.open.repository.OnCallStreamEvent
import github.leavesczy.compose_chat.open.repository.OpenOnCallRepository
import androidx.lifecycle.viewModelScope
import github.leavesczy.compose_chat.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * AI OnCall 聊天页面 ViewModel
 *
 * 职责：
 * - 管理聊天会话的完整生命周期
 * - 暴露 [uiState] StateFlow 供页面订阅
 * - 封装消息发送、流式接收、停止、重试等全部业务逻辑
 *
 * 页面只需：
 * 1. collectAsState(uiState) 获取状态
 * 2. 调用 onInputChange / sendMessage / stop / retry 转发用户操作
 */
class OnCallViewModel : BaseViewModel() {

    private val repository = OpenOnCallRepository()

    private val _uiState = MutableStateFlow(OnCallUiState())
    val uiState: StateFlow<OnCallUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    // ==================== 用户操作 ====================

    /** 输入框文本变化 */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    /** 发送输入框中的消息 */
    fun sendMessage() {
        doSend(content = _uiState.value.input.trim())
    }

    /** 发送快捷问题（零秒发送，无需经过输入框） */
    fun sendQuickMessage(text: String) {
        doSend(content = text.trim())
    }

    /** 停止当前生成 */
    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val streamingIdx = messages.indexOfLast { msg ->
                msg is OnCallMessageUi.Assistant && msg.streaming
            }
            if (streamingIdx != -1) {
                val old = messages[streamingIdx] as OnCallMessageUi.Assistant
                if (old.content.isBlank()) {
                    messages.removeAt(streamingIdx)
                } else {
                    messages[streamingIdx] = old.copy(streaming = false)
                }
            }
            state.copy(messages = messages, isSending = false, isStreaming = false)
        }
    }

    /** 重试上一条失败消息 */
    fun retryLastMessage() {
        val state = _uiState.value
        val lastUserMessage = state.messages.findLast { it is OnCallMessageUi.User } as? OnCallMessageUi.User
            ?: return
        doSend(content = lastUserMessage.content)
    }

    /** 清空对话 */
    fun clearConversation() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { OnCallUiState() }
    }

    // ==================== 内部实现 ====================

    private fun doSend(content: String) {
        if (content.isBlank() || _uiState.value.isSending) return

        val userId = UUID.randomUUID().toString()
        val assistantId = UUID.randomUUID().toString()

        _uiState.update {
            it.copy(
                messages = it.messages +
                        OnCallMessageUi.User(id = userId, content = content) +
                        OnCallMessageUi.Assistant(
                            id = assistantId,
                            content = "",
                            streaming = true,
                            failedMessage = null
                        ),
                input = "",
                isSending = true,
                isStreaming = true,
                currentIntent = null,
                error = null
            )
        }

        streamJob = viewModelScope.launch {
            repository.stream(message = content).collect { event ->
                when (event) {
                    is OnCallStreamEvent.Delta -> onDelta(assistantId, event.text)
                    is OnCallStreamEvent.Intent -> onIntent(event.intent)
                    is OnCallStreamEvent.Tool -> onTool(event)
                    is OnCallStreamEvent.Done -> onDone(assistantId)
                    is OnCallStreamEvent.Error -> onError(assistantId, event.message)
                    is OnCallStreamEvent.Unknown -> { /* 未知事件不打断主流程 */ }
                }
            }
        }
    }

    private fun onDelta(assistantId: String, text: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfFirst { it.id == assistantId }
            if (idx != -1) {
                val old = messages[idx] as OnCallMessageUi.Assistant
                messages[idx] = old.copy(content = old.content + text)
            }
            state.copy(messages = messages)
        }
    }

    private fun onIntent(rawIntent: String) {
        val displayIntent = rawIntent.toDisplayIntent()
        _uiState.update { it.copy(currentIntent = displayIntent) }
    }

    // ==================== Intent 映射 ====================

    companion object {

        /**
         * 将 AI Service 返回的原始 Intent 枚举映射为用户可见的中文标签。
         * 未知值统一降级为"AI 助手"。
         */
        fun String.toDisplayIntent(): String = when (this) {
            "PROTOCOL_QA" -> "协议问答"
            "ERROR_DIAGNOSIS" -> "错误诊断"
            "CODE_GENERATION" -> "代码生成"
            "GENERAL_CHAT" -> "通用助手"
            else -> "AI 助手"
        }
    }

    private fun onTool(event: OnCallStreamEvent.Tool) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + OnCallMessageUi.Tool(
                    id = UUID.randomUUID().toString(),
                    tool = event.tool
                )
            )
        }
    }

    private fun onDone(assistantId: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfFirst { it.id == assistantId }
            if (idx != -1) {
                val old = messages[idx] as OnCallMessageUi.Assistant
                messages[idx] = old.copy(streaming = false)
            }
            state.copy(messages = messages, isSending = false, isStreaming = false)
        }
    }

    private fun onError(assistantId: String, message: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfFirst { it.id == assistantId }
            if (idx != -1) {
                val old = messages[idx] as OnCallMessageUi.Assistant
                messages[idx] = old.copy(
                    streaming = false,
                    failedMessage = message
                )
            }
            state.copy(
                messages = messages,
                isSending = false,
                isStreaming = false,
                error = message
            )
        }
    }

    // ==================== 生命周期 ====================

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        streamJob = null
    }
}



