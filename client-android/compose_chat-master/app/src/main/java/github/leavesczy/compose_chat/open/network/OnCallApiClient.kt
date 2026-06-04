package github.leavesczy.compose_chat.open.network

import github.leavesczy.compose_chat.open.config.OnCallConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI Service 专用 HTTP 客户端
 *
 * 职责边界：
 * - 仅负责与 AI Service 的 HTTP/SSE 通信
 * - 不依赖 OpenApiClient、OpenApiConfig、OpenSessionManager
 * - 与 Container Server 网络层完全解耦
 *
 * 超时设计：
 * - connectTimeout = 30s（允许远程推理服务建连波动）
 * - readTimeout = 5min（匹配 LLM 流式输出最长会话）
 * - writeTimeout = 30s（请求体极短，瞬时完成）
 *
 * 鉴权说明：
 * - 当前不携带鉴权头。后续如需 API Key 或独立 Token，
 *   通过构造函数注入并在 buildRequest 中添加 Header 即可。
 */
class OnCallApiClient(
    private val baseUrl: String = OnCallConfig.AI_SERVICE_BASE_URL
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(OnCallConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(OnCallConfig.READ_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .writeTimeout(OnCallConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 发起 SSE 流式 GET 请求
     *
     * 适用场景：简单查询参数场景，当前 Repository 的过渡方案。
     * 长远推荐使用 ssePost（POST + JSON body），更符合 AI Service 协议。
     */
    fun sseGet(path: String): Flow<OnCallSseLine> {
        val request = buildRequest(path = path)
            .header("Accept", "text/event-stream")
            .get()
            .build()
        return executeSse(request)
    }

    /**
     * 发起 SSE 流式 POST 请求
     *
     * 请求体为 JSON，携带 message 与可选的 conversationId。
     * 这是 AI Service /api/chat/stream 的标准调用方式。
     */
    fun ssePost(path: String, json: JSONObject): Flow<OnCallSseLine> {
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = buildRequest(path = path)
            .header("Accept", "text/event-stream")
            .post(body)
            .build()
        return executeSse(request)
    }

    private fun buildRequest(path: String): Request.Builder {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return Request.Builder()
            .url("$normalizedBaseUrl$normalizedPath")
    }

    private fun executeSse(request: Request): Flow<OnCallSseLine> {
        return flow {
            var retryCount = 0
            val maxRetries = 3
            var lastError: OnCallSseLine.Error? = null

            while (retryCount <= maxRetries) {
                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            lastError = OnCallSseLine.Error(
                                code = "HTTP_${response.code}",
                                message = "AI Service error: ${response.message}"
                            )
                            if (retryCount < maxRetries) {
                                retryCount++
                                val delayMs = (1000L * Math.pow(2.0, (retryCount - 1).toDouble())).toLong()
                                kotlinx.coroutines.delay(delayMs)
                                continue
                            } else {
                                emit(lastError!!)
                                return@flow
                            }
                        }
                        val source = response.body.source()
                        while (!source.exhausted()) {
                            emit(OnCallSseLine.Text(line = source.readUtf8Line().orEmpty()))
                        }
                        // Normal completion, no retry needed
                        return@flow
                    }
                } catch (error: IOException) {
                    retryCount++
                    lastError = OnCallSseLine.Error(
                        code = "NETWORK_ERROR",
                        message = error.message ?: "Network error"
                    )
                    if (retryCount <= maxRetries) {
                        val delayMs = (1000L * Math.pow(2.0, (retryCount - 1).toDouble())).toLong()
                        emit(OnCallSseLine.Error(
                            code = "RETRY_${retryCount}",
                            message = "Reconnecting in ${delayMs}ms..."
                        ))
                        kotlinx.coroutines.delay(delayMs)
                    } else {
                        emit(lastError!!)
                        return@flow
                    }
                }
            }
        }.flowOn(context = Dispatchers.IO)
    }
}

/**
 * SSE 流中的一行数据
 *
 * 与 OpenSseLine 语义等价但类型独立，避免引入 Container Server 网络层的依赖。
 */
sealed class OnCallSseLine {

    data class Text(val line: String) : OnCallSseLine()

    data class Error(val code: String, val message: String) : OnCallSseLine()
}

