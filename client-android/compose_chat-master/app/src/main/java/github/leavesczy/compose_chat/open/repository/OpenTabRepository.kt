package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.open.model.ApprovalSummaryResponse
import github.leavesczy.compose_chat.open.model.CreateCustomWebTabRequest
import github.leavesczy.compose_chat.open.model.SuccessResponse
import github.leavesczy.compose_chat.open.model.TabManifest
import github.leavesczy.compose_chat.open.model.TabMutationResponse
import github.leavesczy.compose_chat.open.model.UpdateCustomWebTabRequest
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabRegistry
import github.leavesczy.compose_chat.open.tab.OpenTabSource
import github.leavesczy.compose_chat.open.tab.OpenTabState
import org.json.JSONObject

class OpenTabRepository(
    private val apiClient: OpenApiClient = OpenApiClient(),
    private val sessionManager: OpenSessionManager = OpenSessionManager
) {

    suspend fun getRemoteTabs(): OpenApiResult<List<TabManifest>> {
        return apiClient.get(path = "/tabs").map(OpenJsonParser::parseTabs)
    }

    suspend fun getCatalogTabs(): OpenApiResult<List<TabManifest>> {
        return apiClient.get(path = "/tabs/catalog").map(OpenJsonParser::parseTabs)
    }

    suspend fun enableTab(tabId: String): OpenApiResult<SuccessResponse> {
        val body = JSONObject()
            .put("tabId", tabId)
        return apiClient.postJson(path = "/me/tabs", json = body).map(OpenJsonParser::parseSuccess)
    }

    suspend fun disableTab(tabId: String): OpenApiResult<SuccessResponse> {
        return apiClient.delete(path = "/me/tabs/$tabId").map(OpenJsonParser::parseSuccess)
    }

    suspend fun createCustomWebTab(request: CreateCustomWebTabRequest): OpenApiResult<TabMutationResponse> {
        val body = JSONObject()
            .put("id", request.id)
            .put("displayName", request.displayName)
            .put("description", request.description)
            .put("icon", request.icon)
            .put("route", request.route)
            .put("entryType", "web")
            .put("entryUri", request.entryUri)
            .put("minContainerVersion", request.minContainerVersion)
        return apiClient.postJson(path = "/tabs", json = body).map(OpenJsonParser::parseTabMutation)
    }

    suspend fun updateCustomWebTab(
        tabId: String,
        request: UpdateCustomWebTabRequest
    ): OpenApiResult<TabMutationResponse> {
        val body = JSONObject()
            .put("displayName", request.displayName)
            .put("description", request.description)
            .put("icon", request.icon)
            .put("entryUri", request.entryUri)
        request.sortOrder?.let { sortOrder ->
            body.put("sortOrder", sortOrder)
        }
        return apiClient.putJson(path = "/tabs/$tabId", json = body).map(OpenJsonParser::parseTabMutation)
    }

    suspend fun deleteCustomTab(tabId: String): OpenApiResult<SuccessResponse> {
        return apiClient.delete(path = "/tabs/$tabId").map(OpenJsonParser::parseSuccess)
    }

    suspend fun getApprovalSummary(): OpenApiResult<ApprovalSummaryResponse> {
        return apiClient.get(path = "/business/approval/summary").map(OpenJsonParser::parseApprovalSummary)
    }

    fun getMockTabs(): List<OpenTabItem> {
        return buildItems(manifests = OpenMockData.tabs(), remoteLoaded = false)
    }

    fun buildItems(manifests: List<TabManifest>, remoteLoaded: Boolean = true): List<OpenTabItem> {
        val permissions = sessionManager.permissions.ifEmpty {
            OpenMockData.defaultPermissions
        }
        val normalizedManifests = manifests.withClientBuiltInTabs()
        return normalizedManifests.sortedBy { it.sortOrder }.map { manifest ->
            OpenTabItem(
                manifest = manifest,
                openState = resolveState(manifest = manifest, permissions = permissions),
                icon = OpenTabRegistry.iconOf(icon = manifest.icon),
                source = manifest.resolveSource(remoteLoaded = remoteLoaded)
            )
        }
    }

    private fun TabManifest.resolveSource(remoteLoaded: Boolean): OpenTabSource {
        if (id == CLIENT_BUILT_IN_AI_ONCALL || id == CLIENT_BUILT_IN_BILIBILI_WEB) {
            return OpenTabSource.ClientBuiltIn
        }
        return if (remoteLoaded) {
            OpenTabSource.Remote
        } else {
            OpenTabSource.LocalMock
        }
    }

    private fun resolveState(
        manifest: TabManifest,
        permissions: Set<String>
    ): OpenTabState {
        if (manifest.id.isBlank() || manifest.displayName.isBlank() || manifest.route.isBlank()) {
            return OpenTabState.InvalidConfig
        }
        if (!manifest.enabled) {
            return OpenTabState.Disabled
        }
        if (manifest.minContainerVersion > OpenApiConfig.CONTAINER_VERSION) {
            return OpenTabState.VersionIncompatible
        }
        if (!permissions.containsAll(manifest.permissions)) {
            return OpenTabState.PermissionDenied
        }
        if (!OpenTabRegistry.supportsEntryType(entryType = manifest.entryType)) {
            return OpenTabState.EntryUnsupported
        }
        if (!OpenTabRegistry.supportsRoute(route = manifest.route)) {
            return OpenTabState.RouteUnsupported
        }
        return OpenTabState.Openable
    }

    private fun List<TabManifest>.withClientBuiltInTabs(): List<TabManifest> {
        var tabs = this
        if (tabs.none { it.id == CLIENT_BUILT_IN_BILIBILI_WEB }) {
            // Web Tab 是开放容器最直观的演示能力。服务端接口补齐前，客户端内置一个 B站首页示例；
            // 后续服务端下发同 id 时自动使用服务端版本，避免客户端和服务端重复展示。
            tabs = tabs + OpenMockData.bilibiliWebTab()
        }
        if (tabs.none { it.id == CLIENT_BUILT_IN_AI_ONCALL }) {
            // 当前服务端 /tabs 尚未下发 AI oncall，但客户端底部已将它作为一级入口。
            // 先以内置 Tab 补齐入口，后续服务端补充 ai-oncall 后这里会自动去重并使用服务端配置。
            tabs = tabs + OpenMockData.aiOncallTab()
        }
        return tabs
    }

    private companion object {

        const val CLIENT_BUILT_IN_AI_ONCALL = "ai-oncall"
        const val CLIENT_BUILT_IN_BILIBILI_WEB = "bilibili-web"

    }

}
