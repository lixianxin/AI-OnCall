package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser
import github.leavesczy.compose_chat.open.network.OpenSseLine
import github.leavesczy.compose_chat.open.network.urlEncode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OpenOnCallRepository(
    private val apiClient: OpenApiClient = OpenApiClient()
) {

    fun stream(message: String): Flow<OnCallStreamEvent> {
        val path = "/oncall/stream?message=${message.urlEncode()}"
        return flow {
            var eventName: String? = null
            apiClient.sse(path = path).collect { line ->
                when (line) {
                    is OpenSseLine.Error -> {
                        emit(OnCallStreamEvent.Error(code = line.result.code, message = line.result.message))
                    }

                    is OpenSseLine.Text -> {
                        val text = line.line.trimEnd()
                        when {
                            text.startsWith("event:") -> {
                                eventName = text.removePrefix("event:").trim()
                            }

                            text.startsWith("data:") -> {
                                val data = text.removePrefix("data:").trim()
                                // 当前云端 Mock 服务只返回 delta/done；接口规划中会扩展 tool/error。
                                // Repository 在这里完成兼容，避免 UI 依赖 SSE 原始文本格式。
                                emit(parseEvent(eventName = eventName, data = data))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseEvent(eventName: String?, data: String): OnCallStreamEvent {
        return runCatching {
            when (eventName) {
                "delta" -> OnCallStreamEvent.Delta(text = OpenJsonParser.parseOnCallDelta(data = data))
                "tool" -> OnCallStreamEvent.Tool(tool = OpenJsonParser.parseOnCallTool(data = data))
                "done" -> OnCallStreamEvent.Done(messageId = OpenJsonParser.parseOnCallDoneMessageId(data = data))
                "error" -> {
                    val error = OpenJsonParser.parseError(json = data)
                    OnCallStreamEvent.Error(code = error.code, message = error.message)
                }

                else -> OnCallStreamEvent.Unknown(eventName = eventName.orEmpty(), data = data)
            }
        }.getOrElse { error ->
            OnCallStreamEvent.Error(
                code = "SSE_PARSE_ERROR",
                message = error.message ?: "AI 流式响应解析失败"
            )
        }
    }

}

sealed class OnCallStreamEvent {

    data class Delta(val text: String) : OnCallStreamEvent()

    data class Tool(val tool: OnCallToolEvent) : OnCallStreamEvent()

    data class Done(val messageId: String?) : OnCallStreamEvent()

    data class Error(val code: String, val message: String) : OnCallStreamEvent()

    data class Unknown(val eventName: String, val data: String) : OnCallStreamEvent()

}
