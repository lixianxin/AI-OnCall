package github.leavesczy.compose_chat.open.network

import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
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
