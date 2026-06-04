package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.config.OnCallConfig
import github.leavesczy.compose_chat.open.model.OnCallMessageDto
import github.leavesczy.compose_chat.open.model.OnCallSessionDto
import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.model.SuccessResponse
import github.leavesczy.compose_chat.open.network.OnCallApiClient
import github.leavesczy.compose_chat.open.network.OnCallSseLine
import github.leavesczy.compose_chat.open.network.OpenApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

class OpenOnCallRepository(
    private val apiClient: OnCallApiClient = OnCallApiClient()
) {
    // In-memory session store (for demo/mock)
    private val sessions = mutableListOf<OnCallSessionDto>()

    /** 閸掓鍤幍鈧張澶婎嚠鐠囨繀绱扮拠?*/
    suspend fun sessions(): OpenApiResult<List<OnCallSessionDto>> {
        return withContext(Dispatchers.IO) {
            OpenApiResult.Success(data = sessions.toList().sortedByDescending { it.updatedAt ?: it.createdAt })
        }
    }

    /** 閸掓稑缂撻弬棰佺窗鐠?*/
    suspend fun createSession(title: String): OpenApiResult<OnCallSessionDto> {
        return withContext(Dispatchers.IO) {
            val now = Instant.now().toString()
            val session = OnCallSessionDto(
                sessionId = "session-" + System.currentTimeMillis(),
                title = title,
                createdAt = now,
                updatedAt = now,
                messageCount = 0
            )
            sessions.add(0, session)
            OpenApiResult.Success(data = session)
        }
    }

    /** 閸掔娀娅庢导姘崇樈 */
    suspend fun deleteSession(sessionId: String): OpenApiResult<SuccessResponse> {
        return withContext(Dispatchers.IO) {
            sessions.removeAll { it.sessionId == sessionId }
            OpenApiResult.Success(data = SuccessResponse(success = true))
        }
    }

    /** 閼惧嘲褰囨导姘崇樈閻ㄥ嫭绉烽幁顖氬坊閸欒绱欓弳鍌濈箲閸ョ偟鈹栭敍灞芥倵缁旑垰鐨婚張顏勭杽閻滄澘鐡ㄩ崒顭掔礆 */
    suspend fun messages(sessionId: String): OpenApiResult<List<OnCallMessageDto>> {
        return withContext(Dispatchers.IO) {
            OpenApiResult.Success(data = emptyList())
        }
    }

    /** 閸氭垵鍑￠張澶夌窗鐠囨繂褰傞柅浣圭Х閹垽绱濇潻鏂挎礀濮濄倖绉烽幁顖滄畱 DTO */
    suspend fun postMessage(sessionId: String, content: String): OpenApiResult<OnCallMessageDto> {
        return withContext(Dispatchers.IO) {
            val now = Instant.now().toString()
            val msg = OnCallMessageDto(
                messageId = "msg-" + System.currentTimeMillis(),
                sessionId = sessionId,
                role = "user",
                content = content,
                contentType = "text",
                createdAt = now
            )
            OpenApiResult.Success(data = msg)
        }
    }

    /** 閺冪姳绱扮拠婵嚹佸蹇曟畱 SSE 濞?*/
    fun stream(message: String): Flow<OnCallStreamEvent> {
        val body = JSONObject().apply {
            put("message", message)
            put("conversationId", "android-" + System.currentTimeMillis())
        }
        return sseFlow(body)
    }

    /** 閸╄桨绨导姘崇樈閻?SSE 濞?*/
    fun stream(sessionId: String, messageId: String, message: String = ""): Flow<OnCallStreamEvent> {
        val body = JSONObject().apply {
            put("message", message)  // message content passed directly
            put("conversationId", sessionId)
            put("messageId", messageId)
        }
        return sseFlow(body)
    }

    private fun sseFlow(body: JSONObject): Flow<OnCallStreamEvent> {
        return flow {
            apiClient.ssePost(path = OnCallConfig.STREAM_CHAT_API, json = body)
                .buffer(8)
                .collect { line ->
                when (line) {
                    is OnCallSseLine.Error -> {
                        // RETRY_* events indicate SSE reconnect, not real errors
                        if (line.code.startsWith("RETRY_")) {
                            android.util.Log.d("OnCallSSE", "SSE retry: " + line.message)
                            return@collect
                        }
                        emit(OnCallStreamEvent.Error(code = line.code, message = line.message))
                    }
                    is OnCallSseLine.Text -> {
                        val text = line.line.trimEnd()
                        if (text.isBlank()) return@collect
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
                "sources" -> {
                    val arr = obj.optJSONArray("sources")
                    val list = if (arr != null) {
                        (0 until arr.length()).mapNotNull { i ->
                            val item = arr.optJSONObject(i) ?: return@mapNotNull null
                            OnCallStreamEvent.SourceItem(
                                file = item.optString("file", ""),
                                relevance = item.optDouble("relevance", 0.0),
                                snippet = item.optString("snippet", "")
                            )
                        }
                    } else emptyList()
                    OnCallStreamEvent.Sources(items = list)
                }
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
            OnCallStreamEvent.Error(code = "SSE_PARSE_ERROR", message = error.message ?: "SSE parse failed")
        }
    }

}

sealed class OnCallStreamEvent {

    data class Delta(val text: String) : OnCallStreamEvent()
    data class Intent(val intent: String) : OnCallStreamEvent()
    data class Tool(val tool: OnCallToolEvent) : OnCallStreamEvent()
    data class Done(val messageId: String?) : OnCallStreamEvent()
    data class Error(val code: String, val message: String) : OnCallStreamEvent()
    data class SourceItem(val file: String, val relevance: Double, val snippet: String)
    data class Sources(val items: List<SourceItem>) : OnCallStreamEvent()
    data class Unknown(val eventName: String, val data: String) : OnCallStreamEvent()

}