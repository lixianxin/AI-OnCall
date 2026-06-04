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
 * AI OnCall 鑱婂ぉ椤甸潰 ViewModel
 *
 * 鑱岃矗锛?
 * - 绠＄悊鑱婂ぉ浼氳瘽鐨勫畬鏁寸敓鍛藉懆鏈?
 * - 鏆撮湶 [uiState] StateFlow 渚涢〉闈㈣闃?
 * - 灏佽娑堟伅鍙戦€併€佹祦寮忔帴鏀躲€佸仠姝€侀噸璇曠瓑鍏ㄩ儴涓氬姟閫昏緫
 *
 * 椤甸潰鍙渶锛?
 * 1. collectAsState(uiState) 鑾峰彇鐘舵€?
 * 2. 璋冪敤 onInputChange / sendMessage / stop / retry 杞彂鐢ㄦ埛鎿嶄綔
 */
class OnCallViewModel : BaseViewModel() {

    private val repository = OpenOnCallRepository()
    private val conversationId = "android-" + System.currentTimeMillis()

    private val _uiState = MutableStateFlow(OnCallUiState())
    val uiState: StateFlow<OnCallUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    // ==================== 鐢ㄦ埛鎿嶄綔 ====================

    /** 杈撳叆妗嗘枃鏈彉鍖?*/
    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    /** 鍙戦€佽緭鍏ユ涓殑娑堟伅 */
    fun sendMessage() {
        doSend(content = _uiState.value.input.trim())
    }

    /** 鍙戦€佸揩鎹烽棶棰橈紙闆剁鍙戦€侊紝鏃犻渶缁忚繃杈撳叆妗嗭級 */
    fun sendQuickMessage(text: String) {
        doSend(content = text.trim())
    }

    /** 鍋滄褰撳墠鐢熸垚 */
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

    /** 閲嶈瘯涓婁竴鏉″け璐ユ秷鎭?*/
    fun retryLastMessage() {
        val state = _uiState.value
        val lastUserMessage = state.messages.findLast { it is OnCallMessageUi.User } as? OnCallMessageUi.User
            ?: return
        doSend(content = lastUserMessage.content)
    }

    /** 娓呯┖瀵硅瘽 */
    fun clearConversation() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { OnCallUiState() }
    }

    // ==================== 鍐呴儴瀹炵幇 ====================

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
            repository.stream(sessionId = conversationId, messageId = UUID.randomUUID().toString(), message = content).collect { event ->
                when (event) {
                    is OnCallStreamEvent.Delta -> onDelta(assistantId, event.text)
                    is OnCallStreamEvent.Intent -> onIntent(event.intent)
                    is OnCallStreamEvent.Tool -> onTool(event)
                    is OnCallStreamEvent.Done -> onDone(assistantId)
                    is OnCallStreamEvent.Error -> onError(assistantId, event.message)
                    is OnCallStreamEvent.Unknown -> { /* 鏈煡浜嬩欢涓嶆墦鏂富娴佺▼ */ }
                    is OnCallStreamEvent.Sources -> { /* sources not shown in this view */ }
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

    // ==================== Intent 鏄犲皠 ====================

    companion object {

        /**
         * 灏?AI Service 杩斿洖鐨勫師濮?Intent 鏋氫妇鏄犲皠涓虹敤鎴峰彲瑙佺殑涓枃鏍囩銆?
         * 鏈煡鍊肩粺涓€闄嶇骇涓?AI 鍔╂墜"銆?
         */
        fun String.toDisplayIntent(): String = when (this) {
            "PROTOCOL_QA" -> "鍗忚闂瓟"
            "ERROR_DIAGNOSIS" -> "閿欒璇婃柇"
            "CODE_GENERATION" -> "浠ｇ爜鐢熸垚"
            "GENERAL_CHAT" -> "閫氱敤鍔╂墜"
            else -> "AI 鍔╂墜"
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

    // ==================== 鐢熷懡鍛ㄦ湡 ====================

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        streamJob = null
    }
}



