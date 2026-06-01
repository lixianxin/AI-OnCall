package github.leavesczy.compose_chat.open.network

import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class OpenApiClient(
    private val baseUrl: String = OpenApiConfig.DEFAULT_BASE_URL,
    private val sessionManager: OpenSessionManager = OpenSessionManager
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    suspend fun get(path: String, authRequired: Boolean = true): OpenApiResult<String> {
        val request = baseRequest(path = path, authRequired = authRequired)
            .get()
            .build()
        return execute(request = request)
    }

    suspend fun postJson(
        path: String,
        json: JSONObject,
        authRequired: Boolean = true
    ): OpenApiResult<String> {
        val requestBody = json.toString().toRequestBody(jsonMediaType)
        val request = baseRequest(path = path, authRequired = authRequired)
            .post(requestBody)
            .build()
        return execute(request = request)
    }

    suspend fun putJson(
        path: String,
        json: JSONObject,
        authRequired: Boolean = true
    ): OpenApiResult<String> {
        val requestBody = json.toString().toRequestBody(jsonMediaType)
        val request = baseRequest(path = path, authRequired = authRequired)
            .put(requestBody)
            .build()
        return execute(request = request)
    }

    suspend fun delete(path: String, authRequired: Boolean = true): OpenApiResult<String> {
        val request = baseRequest(path = path, authRequired = authRequired)
            .delete()
            .build()
        return execute(request = request)
    }

    fun sse(path: String, authRequired: Boolean = true): Flow<OpenSseLine> {
        val request = baseRequest(path = path, authRequired = authRequired)
            .header("Accept", "text/event-stream")
            .get()
            .build()
        return flow {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        emit(
                            OpenSseLine.Error(
                                result = OpenJsonParser.parseError(json = response.body.string())
                            )
                        )
                        return@use
                    }
                    val source = response.body.source()
                    while (!source.exhausted()) {
                        // SSE 是按行推送的协议。这里保持“逐行透传”，由 Repository 按 event/data 聚合，
                        // 这样未来服务端新增 event 类型时，网络层无需理解业务语义。
                        emit(OpenSseLine.Text(line = source.readUtf8Line().orEmpty()))
                    }
                }
            } catch (error: IOException) {
                emit(
                    OpenSseLine.Error(
                        result = OpenApiResult.Failed(
                            code = "NETWORK_ERROR",
                            message = error.message ?: "Network error"
                        )
                    )
                )
            }
        }.flowOn(context = Dispatchers.IO)
    }

    private fun baseRequest(path: String, authRequired: Boolean): Request.Builder {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val builder = Request.Builder()
            .url("$normalizedBaseUrl$normalizedPath")
            .header("Accept", "application/json")
        if (authRequired && sessionManager.token.isNotBlank()) {
            builder.header("Authorization", "Bearer ${sessionManager.token}")
        }
        return builder
    }

    private suspend fun execute(request: Request): OpenApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    if (response.isSuccessful) {
                        OpenApiResult.Success(data = body)
                    } else {
                        OpenJsonParser.parseError(json = body)
                    }
                }
            } catch (error: IOException) {
                OpenApiResult.Failed(code = "NETWORK_ERROR", message = error.message ?: "Network error")
            }
        }
    }

    private companion object {

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        const val timeoutSeconds = 15L

    }

}

sealed class OpenSseLine {

    data class Text(val line: String) : OpenSseLine()

    data class Error(val result: OpenApiResult.Failed) : OpenSseLine()

}

fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}
