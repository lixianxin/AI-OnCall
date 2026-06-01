package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.OnCallMessageDto
import github.leavesczy.compose_chat.open.model.OnCallSessionDto
import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser
import github.leavesczy.compose_chat.open.network.OpenSseLine
import github.leavesczy.compose_chat.open.network.urlEncode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

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

    suspend fun createSession(title: String): OpenApiResult<OnCallSessionDto> {
        val body = JSONObject().put("title", title)
        return apiClient.postJson(path = "/oncall/sessions", json = body).map(OpenJsonParser::parseOnCallSession)
    }

    suspend fun sessions(): OpenApiResult<List<OnCallSessionDto>> {
        return apiClient.get(path = "/oncall/sessions").map(OpenJsonParser::parseOnCallSessions)
    }

    suspend fun messages(sessionId: String): OpenApiResult<List<OnCallMessageDto>> {
        return apiClient.get(path = "/oncall/sessions/$sessionId/messages").map(OpenJsonParser::parseOnCallMessages)
    }

    suspend fun postMessage(
        sessionId: String,
        content: String,
        contentType: String = "text"
    ): OpenApiResult<OnCallMessageDto> {
        val body = JSONObject()
            .put("content", content)
            .put("contentType", contentType)
        return apiClient.postJson(path = "/oncall/sessions/$sessionId/messages", json = body)
            .map(OpenJsonParser::parseOnCallMessage)
    }

    fun stream(sessionId: String, messageId: String): Flow<OnCallStreamEvent> {
        val path = "/oncall/sessions/$sessionId/stream?messageId=${messageId.urlEncode()}"
        return streamPath(path = path)
    }

    suspend fun deleteSession(sessionId: String): OpenApiResult<github.leavesczy.compose_chat.open.model.SuccessResponse> {
        return apiClient.delete(path = "/oncall/sessions/$sessionId").map { json ->
            val obj = JSONObject(json)
            github.leavesczy.compose_chat.open.model.SuccessResponse(
                success = obj.optBoolean("success"),
                tabId = obj.optString("sessionId").ifBlank { null }
            )
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

    private fun streamPath(path: String): Flow<OnCallStreamEvent> {
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
                                emit(parseEvent(eventName = eventName, data = data))
                            }
                        }
                    }
                }
            }
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
