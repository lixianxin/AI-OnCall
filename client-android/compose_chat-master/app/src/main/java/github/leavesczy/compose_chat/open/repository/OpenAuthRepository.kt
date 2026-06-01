package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.DebugStatusResponse
import github.leavesczy.compose_chat.open.model.HealthResponse
import github.leavesczy.compose_chat.open.model.LoginRequest
import github.leavesczy.compose_chat.open.model.LoginResponse
import github.leavesczy.compose_chat.open.model.MeResponse
import github.leavesczy.compose_chat.open.model.RegisterRequest
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import org.json.JSONObject

class OpenAuthRepository(
    private val apiClient: OpenApiClient = OpenApiClient(),
    private val sessionManager: OpenSessionManager = OpenSessionManager
) {

    suspend fun health(): OpenApiResult<HealthResponse> {
        return apiClient.get(path = "/health", authRequired = false).map(OpenJsonParser::parseHealth)
    }

    suspend fun login(request: LoginRequest): OpenApiResult<LoginResponse> {
        val body = JSONObject()
            .put("account", request.account)
            .put("password", request.password)
        val result = apiClient.postJson(path = "/auth/login", json = body, authRequired = false)
            .map(OpenJsonParser::parseLogin)
        if (result is OpenApiResult.Success) {
            sessionManager.saveSession(
                token = result.data.token,
                userId = result.data.userId,
                displayName = result.data.displayName,
                permissions = result.data.permissions
            )
        }
        return result
    }

    suspend fun register(request: RegisterRequest): OpenApiResult<LoginResponse> {
        val body = JSONObject()
            .put("account", request.account)
            .put("password", request.password)
            .put("displayName", request.displayName)
        val result = apiClient.postJson(path = "/auth/register", json = body, authRequired = false)
            .map(OpenJsonParser::parseLogin)
        if (result is OpenApiResult.Success) {
            sessionManager.saveSession(
                token = result.data.token,
                userId = result.data.userId,
                displayName = result.data.displayName,
                permissions = result.data.permissions
            )
        }
        return result
    }

    suspend fun me(): OpenApiResult<MeResponse> {
        return apiClient.get(path = "/me").map(OpenJsonParser::parseMe)
    }

    suspend fun debugStatus(): OpenApiResult<DebugStatusResponse> {
        return apiClient.get(path = "/debug/status").map(OpenJsonParser::parseDebugStatus)
    }

}
