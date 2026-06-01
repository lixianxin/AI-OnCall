package github.leavesczy.compose_chat.open.network

import github.leavesczy.compose_chat.open.model.ApprovalItemDto
import github.leavesczy.compose_chat.open.model.ApprovalSummaryResponse
import github.leavesczy.compose_chat.open.model.DebugStatusResponse
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.FabExtensionDto
import github.leavesczy.compose_chat.open.model.HealthResponse
import github.leavesczy.compose_chat.open.model.LoginResponse
import github.leavesczy.compose_chat.open.model.MeResponse
import github.leavesczy.compose_chat.open.model.MenuItemDto
import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.model.SemanticVersionDto
import github.leavesczy.compose_chat.open.model.SuccessResponse
import github.leavesczy.compose_chat.open.model.TabExtensionDto
import github.leavesczy.compose_chat.open.model.TabManifest
import github.leavesczy.compose_chat.open.model.TabMutationResponse
import github.leavesczy.compose_chat.open.model.TeamDto
import github.leavesczy.compose_chat.open.model.TitleBarExtensionDto
import org.json.JSONArray
import org.json.JSONObject

object OpenJsonParser {

    fun parseHealth(json: String): HealthResponse {
        val obj = JSONObject(json)
        return HealthResponse(
            status = obj.optString("status"),
            service = obj.optString("service"),
            mode = obj.optString("mode"),
            serverTime = obj.optString("serverTime")
        )
    }

    fun parseLogin(json: String): LoginResponse {
        val obj = JSONObject(json)
        return LoginResponse(
            token = obj.optString("token"),
            userId = obj.optString("userId").ifBlank { null },
            displayName = obj.optString("displayName"),
            permissions = obj.optJSONArray("permissions").toStringList()
        )
    }

    fun parseMe(json: String): MeResponse {
        val obj = JSONObject(json)
        val teamObj = obj.optJSONObject("team")
        return MeResponse(
            userId = obj.optString("userId"),
            displayName = obj.optString("displayName"),
            permissions = obj.optJSONArray("permissions").toStringList(),
            team = teamObj?.let {
                TeamDto(
                    id = it.optString("id"),
                    name = it.optString("name")
                )
            }
        )
    }

    fun parseDebugStatus(json: String): DebugStatusResponse {
        val obj = JSONObject(json)
        return DebugStatusResponse(
            serverTime = obj.optString("serverTime"),
            apiVersion = obj.optString("apiVersion"),
            mockMode = obj.optBoolean("mockMode"),
            sseAvailable = obj.optBoolean("sseAvailable"),
            tabCount = obj.optInt("tabCount")
        )
    }

    fun parseApprovalSummary(json: String): ApprovalSummaryResponse {
        val obj = JSONObject(json)
        val items = obj.optJSONArray("items").mapObjects { item ->
            ApprovalItemDto(
                id = item.optString("id"),
                title = item.optString("title"),
                applicant = item.optString("applicant"),
                status = item.optString("status"),
                createdAt = item.optString("createdAt")
            )
        }
        return ApprovalSummaryResponse(
            pendingCount = obj.optInt("pendingCount"),
            approvedToday = obj.optInt("approvedToday"),
            items = items
        )
    }

    fun parseTabs(json: String): List<TabManifest> {
        return JSONArray(json).mapObjects(::parseTabManifest)
    }

    fun parseSuccess(json: String): SuccessResponse {
        val obj = JSONObject(json)
        return SuccessResponse(
            success = obj.optBoolean("success"),
            tabId = obj.optString("tabId").ifBlank { null }
        )
    }

    fun parseTabMutation(json: String): TabMutationResponse {
        val obj = JSONObject(json)
        return TabMutationResponse(
            success = obj.optBoolean("success"),
            tabId = obj.optString("tabId").ifBlank { null },
            tab = obj.optJSONObject("tab")?.let(::parseTabManifest)
        )
    }

    fun parseOnCallDelta(data: String): String {
        return JSONObject(data).optString("text")
    }

    fun parseOnCallTool(data: String): OnCallToolEvent {
        val obj = JSONObject(data)
        return OnCallToolEvent(
            name = obj.optString("name").ifBlank { "tool" },
            status = obj.optString("status"),
            summary = obj.optString("summary")
        )
    }

    fun parseOnCallDoneMessageId(data: String): String? {
        if (data.isBlank() || data == "{}") {
            return null
        }
        return JSONObject(data).optString("messageId").ifBlank { null }
    }

    fun parseTabManifest(obj: JSONObject): TabManifest {
        return TabManifest(
            id = obj.optString("id"),
            displayName = obj.optString("displayName"),
            description = obj.optString("description").ifBlank { null },
            icon = obj.optString("icon").ifBlank { null },
            route = obj.optString("route"),
            entryType = EntryType.from(obj.optString("entryType")),
            entryUri = obj.optString("entryUri").ifBlank { null },
            version = parseVersion(obj.optJSONObject("version")),
            minContainerVersion = obj.optInt("minContainerVersion", defaultMinContainerVersion),
            permissions = obj.optJSONArray("permissions").toStringList(),
            enabled = obj.optBoolean("enabled", true),
            sortOrder = obj.optInt("sortOrder", Int.MAX_VALUE),
            extension = parseExtension(obj.optJSONObject("extension")),
            extraConfig = obj.optJSONObject("extraConfig").toStringMap()
        )
    }

    fun parseError(json: String?): OpenApiResult.Failed {
        if (json.isNullOrBlank()) {
            return OpenApiResult.Failed(code = "NETWORK_ERROR", message = "Empty error body")
        }
        if (!json.trimStart().startsWith("{")) {
            return OpenApiResult.Failed(code = "HTTP_ERROR", message = json.trim())
        }
        return runCatching {
            val obj = JSONObject(json)
            OpenApiResult.Failed(
                code = obj.optString("code", "UNKNOWN_ERROR"),
                message = obj.optString("message", "Unknown error"),
                traceId = obj.optString("traceId").ifBlank { null }
            )
        }.getOrElse { error ->
            OpenApiResult.Failed(code = "UNKNOWN_ERROR", message = error.message ?: "Unknown error")
        }
    }

    private fun parseVersion(obj: JSONObject?): SemanticVersionDto {
        return SemanticVersionDto(
            major = obj?.optInt("major") ?: 1,
            minor = obj?.optInt("minor") ?: 0,
            patch = obj?.optInt("patch") ?: 0
        )
    }

    private fun parseExtension(obj: JSONObject?): TabExtensionDto? {
        if (obj == null) {
            return null
        }
        return TabExtensionDto(
            titleBar = parseTitleBar(obj.optJSONObject("titleBar")),
            fab = parseFab(obj.optJSONObject("fab"))
        )
    }

    private fun parseTitleBar(obj: JSONObject?): TitleBarExtensionDto? {
        if (obj == null) {
            return null
        }
        return TitleBarExtensionDto(
            rightText = obj.optString("rightText").ifBlank { null },
            menuItems = obj.optJSONArray("menuItems").mapObjects { item ->
                MenuItemDto(
                    id = item.optString("id"),
                    label = item.optString("label")
                )
            }
        )
    }

    private fun parseFab(obj: JSONObject?): FabExtensionDto? {
        if (obj == null) {
            return null
        }
        return FabExtensionDto(
            id = obj.optString("id"),
            icon = obj.optString("icon").ifBlank { null },
            label = obj.optString("label")
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) {
            return emptyList()
        }
        return List(length()) { index ->
            optString(index)
        }.filter { it.isNotBlank() }
    }

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) {
            return emptyMap()
        }
        return keys().asSequence().associateWith { key ->
            opt(key)?.toString().orEmpty()
        }
    }

    private fun <T> JSONArray?.mapObjects(mapper: (JSONObject) -> T): List<T> {
        if (this == null) {
            return emptyList()
        }
        return List(length()) { index ->
            mapper(optJSONObject(index) ?: JSONObject())
        }
    }

    private const val defaultMinContainerVersion = 1

}
