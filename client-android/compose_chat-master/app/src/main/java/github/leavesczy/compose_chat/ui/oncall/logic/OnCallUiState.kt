package github.leavesczy.compose_chat.ui.oncall.logic

import github.leavesczy.compose_chat.open.model.OnCallToolEvent

/**
 * AI OnCall 页面 UI 状态
 *
 * 单一数据源，由 [OnCallViewModel] 通过 StateFlow 驱动。
 * 页面仅负责 collect 并渲染，不持有任何可变状态。
 */
data class OnCallUiState(
    /** 消息列表 */
    val messages: List<OnCallMessageUi> = listOf(welcomeMessage()),
    /** 输入框文本 */
    val input: String = "",
    /** 是否正在发送（等待响应） */
    val isSending: Boolean = false,
    /** 是否正在流式接收 */
    val isStreaming: Boolean = false,
    /** 当前识别到的意图 */
    val currentIntent: String? = null,
    /** 错误信息（非空时显示重试按钮） */
    val error: String? = null
) {

    companion object {

        fun welcomeMessage(): OnCallMessageUi.Assistant {
            return OnCallMessageUi.Assistant(
                id = "welcome",
                content = "你好，我是 AI oncall。你可以把 TabManifest、错误码、日志或接入疑问发给我，我会围绕协议给出建议。",
                streaming = false,
                failedMessage = null
            )
        }
    }
}

/**
 * 聊天消息 UI 模型
 *
 * 从 OpenOnCallPage 内部 sealed class 提取为顶层类型，
 * 供 ViewModel 和 Page 共享使用。
 */
sealed class OnCallMessageUi {

    abstract val id: String

    /** 用户消息 */
    data class User(
        override val id: String,
        val content: String
    ) : OnCallMessageUi()

    /** AI 助手消息 */
    data class Assistant(
        override val id: String,
        val content: String,
        val streaming: Boolean,
        val failedMessage: String?
    ) : OnCallMessageUi()

    /** 工具调用消息 */
    data class Tool(
        override val id: String,
        val tool: OnCallToolEvent
    ) : OnCallMessageUi()
}
