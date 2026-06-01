package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.config.OnCallConfig
import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.network.OnCallApiClient
import github.leavesczy.compose_chat.open.network.OnCallSseLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class OpenOnCallRepository(
    private val apiClient: OnCallApiClient = OnCallApiClient()
) {

    fun stream(message: String): Flow<OnCallStreamEvent> {
        val body = JSONObject().apply {
            put("message", message)
            put("conversationId", "android-" + System.currentTimeMillis())
        }
        return flow {
            apiClient.ssePost(path = OnCallConfig.STREAM_CHAT_API, json = body).collect { line ->
                when (line) {
                    is OnCallSseLine.Error -> {
                        emit(OnCallStreamEvent.Error(code = line.code, message = line.message))
                    }

                    is OnCallSseLine.Text -> {
                        val text = line.line.trimEnd()
                        if (text.isBlank()) {
                            return@collect
                        }
                        val json = if (text.startsWith("data:")) {
                            text.removePrefix("data:").trim()
                        } else if (text.startsWith("{")) {
                            text
                        } else {
                            return@collect
                        }
                        emit(parseSseLine(json))
                    }
                }
            }
        }
    }

    private fun parseSseLine(json: String): OnCallStreamEvent {
        return runCatching {
            val obj = JSONObject(json)
            when (obj.optString("type")) {
                "intent" -> OnCallStreamEvent.Intent(intent = obj.optString("intent", "GENERAL_CHAT"))
                "tool" -> OnCallStreamEvent.Tool(tool = OnCallToolEvent(
                    name = obj.optString("tool", "unknown"),
                    status = obj.optString("status", "running"),
                    summary = obj.optString("summary", "")
                ))
                "content" -> OnCallStreamEvent.Delta(text = obj.optString("delta", ""))
                "done" -> OnCallStreamEvent.Done(messageId = null)
                "error" -> OnCallStreamEvent.Error(
                    code = obj.optString("code", "SERVER_ERROR"),
                    message = obj.optString("delta", obj.optString("message", "Unknown error"))
                )
                else -> OnCallStreamEvent.Unknown(eventName = obj.optString("type", "unknown"), data = json)
            }
        }.getOrElse { error ->
            OnCallStreamEvent.Error(
                code = "SSE_PARSE_ERROR",
                message = error.message ?: "SSE parse failed"
            )
        }
    }

}

sealed class OnCallStreamEvent {

    data class Delta(val text: String) : OnCallStreamEvent()

    data class Intent(val intent: String) : OnCallStreamEvent()

    data class Tool(val tool: OnCallToolEvent) : OnCallStreamEvent()

    data class Done(val messageId: String?) : OnCallStreamEvent()

    data class Error(val code: String, val message: String) : OnCallStreamEvent()

    data class Unknown(val eventName: String, val data: String) : OnCallStreamEvent()

}
