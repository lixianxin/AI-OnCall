package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.open.model.TabManifest
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabRegistry
import github.leavesczy.compose_chat.open.tab.OpenTabState

class OpenTabRepository(
    private val apiClient: OpenApiClient = OpenApiClient(),
    private val sessionManager: OpenSessionManager = OpenSessionManager
) {

    suspend fun getRemoteTabs(): OpenApiResult<List<TabManifest>> {
        return apiClient.get(path = "/tabs").map(OpenJsonParser::parseTabs)
    }

    fun getMockTabs(): List<OpenTabItem> {
        return buildItems(manifests = OpenMockData.tabs())
    }

    fun buildItems(manifests: List<TabManifest>): List<OpenTabItem> {
        val permissions = sessionManager.permissions.ifEmpty {
            OpenMockData.defaultPermissions
        }
        val normalizedManifests = manifests.withClientBuiltInTabs()
        return normalizedManifests.sortedBy { it.sortOrder }.map { manifest ->
            OpenTabItem(
                manifest = manifest,
                openState = resolveState(manifest = manifest, permissions = permissions),
                icon = OpenTabRegistry.iconOf(icon = manifest.icon)
            )
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
        if (any { it.id == CLIENT_BUILT_IN_AI_ONCALL }) {
            return this
        }
        // 当前服务端 /tabs 尚未下发 AI oncall，但客户端底部已将它作为一级入口。
        // 先以内置 Tab 补齐入口，后续服务端补充 ai-oncall 后这里会自动去重并使用服务端配置。
        return this + OpenMockData.aiOncallTab()
    }

    private companion object {

        const val CLIENT_BUILT_IN_AI_ONCALL = "ai-oncall"

    }

}
