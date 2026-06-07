package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.open.model.ApprovalSummaryResponse
import github.leavesczy.compose_chat.open.model.CreateCustomWebTabRequest
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.FabExtensionDto
import github.leavesczy.compose_chat.open.model.MenuItemDto
import github.leavesczy.compose_chat.open.model.OpenBusinessPermissions
import github.leavesczy.compose_chat.open.model.OpenBusinessTabIds
import github.leavesczy.compose_chat.open.model.SemanticVersionDto
import github.leavesczy.compose_chat.open.model.SuccessResponse
import github.leavesczy.compose_chat.open.model.TabExtensionDto
import github.leavesczy.compose_chat.open.model.TabManifest
import github.leavesczy.compose_chat.open.model.TabMutationResponse
import github.leavesczy.compose_chat.open.model.TabVisibilityRequest
import github.leavesczy.compose_chat.open.model.TitleBarExtensionDto
import github.leavesczy.compose_chat.open.model.UpdateCustomWebTabRequest
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.open.tab.OpenTabContainer
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabRegistry
import github.leavesczy.compose_chat.open.tab.OpenTabSource
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.open.tab.RegisteredOpenTab
import org.json.JSONArray
import org.json.JSONObject

class OpenTabRepository(
    private val apiClient: OpenApiClient = OpenApiClient(),
    private val sessionManager: OpenSessionManager = OpenSessionManager
) {

    private val teamBusinessMockRepository = OpenTeamBusinessMockRepository()

    suspend fun getRemoteTabs(): OpenApiResult<List<TabManifest>> {
        return apiClient.get(path = "/tabs").map(OpenJsonParser::parseTabs)
    }

    suspend fun getCatalogTabs(): OpenApiResult<List<TabManifest>> {
        return apiClient.get(path = "/tabs/catalog").map(OpenJsonParser::parseTabs)
    }

    suspend fun enableTab(tabId: String): OpenApiResult<SuccessResponse> {
        sessionManager.markTabEnabled(tabId = tabId)
        val body = JSONObject()
            .put("tabId", tabId)
        val result = apiClient.postJson(path = "/me/tabs", json = body)
            .map(OpenJsonParser::parseSuccess)
            .requireSuccessFlag(defaultMessage = "服务端未启用该业务")
        return result
    }

    suspend fun disableTab(tabId: String): OpenApiResult<SuccessResponse> {
        sessionManager.markTabDisabled(tabId = tabId)
        val result = apiClient.delete(path = "/me/tabs/$tabId")
            .map(OpenJsonParser::parseSuccess)
            .requireSuccessFlag(defaultMessage = "服务端未停用该业务")
        if (result is OpenApiResult.Failed && result.code == "FORBIDDEN") {
            sessionManager.markTabEnabled(tabId = tabId)
        }
        return result
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
        request.sortOrder?.let { sortOrder ->
            body.put("sortOrder", sortOrder)
        }
        if (request.extraConfig.isNotEmpty()) {
            body.put("extraConfig", JSONObject(request.extraConfig))
        }
        request.visibility?.let { visibility ->
            body.put("visibility", visibility.toJson())
        }
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
        request.visibility?.let { visibility ->
            body.put("visibility", visibility.toJson())
        }
        return apiClient.putJson(path = "/tabs/$tabId", json = body).map(OpenJsonParser::parseTabMutation)
    }

    suspend fun deleteCustomTab(tabId: String): OpenApiResult<SuccessResponse> {
        return apiClient.delete(path = "/tabs/$tabId")
            .map(OpenJsonParser::parseSuccess)
            .requireSuccessFlag(defaultMessage = "服务端未删除该业务")
    }

    suspend fun getApprovalSummary(): OpenApiResult<ApprovalSummaryResponse> {
        return apiClient.get(path = "/business/approval/summary").map(OpenJsonParser::parseApprovalSummary)
    }

    fun getMockTabs(): List<OpenTabItem> {
        val profile = teamBusinessMockRepository.currentProfile(account = sessionManager.lastAccount)
        return buildItems(
            manifests = targetBusinessTabs(),
            remoteLoaded = false,
            permissions = profile.permissions.toSet(),
            mergeTargetBusinessTabs = true
        )
    }

    fun buildItems(
        manifests: List<TabManifest>,
        remoteLoaded: Boolean = true,
        permissions: Set<String> = sessionManager.permissions,
        mergeTargetBusinessTabs: Boolean = false
    ): List<OpenTabItem> {
        val sourceManifests = if (mergeTargetBusinessTabs) {
            manifests.withTargetBusinessTabs()
        } else {
            manifests
        }
        val normalizedManifests = sourceManifests.withClientBuiltInTabs()
            .withProtocolRegisteredTabs()
            .withLocalDisabledOverrides()
            .map { manifest -> manifest.withClientDisplayOverrides() }
        return normalizedManifests.sortedBy { it.sortOrder }.map { manifest ->
            val registeredTab = OpenTabContainer.findById(tabId = manifest.id)
            OpenTabItem(
                manifest = manifest,
                openState = resolveState(manifest = manifest, permissions = permissions),
                icon = registeredTab?.definition?.icon ?: OpenTabRegistry.iconOf(icon = manifest.icon),
                source = manifest.resolveSource(remoteLoaded = remoteLoaded)
            )
        }
    }

    private fun TabManifest.resolveSource(remoteLoaded: Boolean): OpenTabSource {
        if (OpenTabContainer.findById(tabId = id) != null) {
            return OpenTabSource.ProtocolRegistered
        }
        if (id == CLIENT_BUILT_IN_AI_ONCALL) {
            return OpenTabSource.ClientBuiltIn
        }
        return if (remoteLoaded) {
            OpenTabSource.Remote
        } else {
            OpenTabSource.LocalMock
        }
    }

    private fun TabManifest.withClientDisplayOverrides(): TabManifest {
        return when (id) {
            OpenBusinessTabIds.Announcements -> copy(
                displayName = "公告",
                description = "查看公司公告和团队公告"
            )
            else -> this
        }
    }

    private fun resolveState(
        manifest: TabManifest,
        permissions: Set<String>
    ): OpenTabState {
        if (
            manifest.id.isBlank() ||
            manifest.displayName.isBlank() ||
            manifest.icon.isNullOrBlank() ||
            manifest.route.isBlank() ||
            !manifest.route.startsWith("/") ||
            manifest.version.major < 0 ||
            manifest.version.minor < 0 ||
            manifest.version.patch < 0
        ) {
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
        if (tabs.none { it.id == CLIENT_BUILT_IN_AI_ONCALL }) {
            // 当前服务端 /tabs 尚未下发 AI oncall，但客户端底部已将它作为一级入口。
            // 先以内置 Tab 补齐入口，后续服务端补充 ai-oncall 后这里会自动去重并使用服务端配置。
            tabs = tabs + OpenMockData.aiOncallTab()
        }
        return tabs
    }

    private fun List<TabManifest>.withProtocolRegisteredTabs(): List<TabManifest> {
        val manifestIds = map { manifest -> manifest.id }.toSet()
        val registeredManifests = OpenTabContainer.getRegisteredOpenTabs()
            .filterNot { registeredTab -> registeredTab.definition.id in manifestIds }
            .mapIndexed { index, registeredTab ->
                registeredTab.toManifest(sortOrder = PROTOCOL_REGISTERED_SORT_ORDER + index)
            }
        return this + registeredManifests
    }

    private fun List<TabManifest>.withTargetBusinessTabs(): List<TabManifest> {
        val remoteIds = map { it.id }.toSet()
        val merged = this + targetBusinessTabs().filterNot { tab -> tab.id in remoteIds }
        return merged.filterNot { tab -> tab.id == "finance" || tab.id == "legacy-hybrid" || tab.id == "future-tab" }
    }

    private fun List<TabManifest>.withLocalDisabledOverrides(): List<TabManifest> {
        val disabledIds = sessionManager.disabledTabIds()
        if (disabledIds.isEmpty()) {
            return this
        }
        return map { manifest ->
            if (manifest.id in disabledIds && manifest.resolveSource(remoteLoaded = true) == OpenTabSource.Remote) {
                manifest.copy(enabled = false)
            } else {
                manifest
            }
        }
    }

    private fun targetBusinessTabs(): List<TabManifest> {
        return listOf(
            targetTab(
                id = OpenBusinessTabIds.CompanyIntro,
                displayName = "公司介绍",
                description = "查看企业背景、组织信息和项目介绍",
                icon = "company",
                route = "/company-intro",
                permissions = listOf(OpenBusinessPermissions.CompanyRead),
                sortOrder = 10
            ),
            targetTab(
                id = OpenBusinessTabIds.Announcements,
                displayName = "公告",
                description = "查看全公司公告和团队公告",
                icon = "announcement",
                route = "/announcements",
                permissions = listOf(OpenBusinessPermissions.AnnouncementRead),
                sortOrder = 20
            ),
            targetTab(
                id = OpenBusinessTabIds.Approval,
                displayName = "审批中心",
                description = "提交审批、查看进度和处理待审批事项",
                icon = "approval",
                route = "/approval",
                permissions = listOf(OpenBusinessPermissions.ApprovalRead),
                sortOrder = 30
            ),
            targetTab(
                id = OpenBusinessTabIds.Calendar,
                displayName = "团队日程",
                description = "查看团队日程、参与日程和全公司日程",
                icon = "calendar",
                route = "/calendar",
                permissions = listOf(OpenBusinessPermissions.CalendarRead),
                sortOrder = 40
            ),
            targetTab(
                id = OpenBusinessTabIds.Fun,
                displayName = "放松一刻",
                description = "团队轻量娱乐和休息入口",
                icon = "fun",
                route = "/fun",
                permissions = listOf(OpenBusinessPermissions.FunRead),
                sortOrder = 50
            ),
            targetTab(
                id = OpenBusinessTabIds.PermissionAdmin,
                displayName = "权限管理",
                description = "管理团队、成员、角色和可见业务入口",
                icon = "admin",
                route = "/permission-admin",
                permissions = listOf(OpenBusinessPermissions.AdminManage),
                sortOrder = 60
            ),
            targetTab(
                id = OpenBusinessTabIds.AiOnCall,
                displayName = "AI oncall",
                description = "提供协议问答、配置诊断和接入建议",
                icon = "ai-oncall",
                route = "/ai-oncall",
                permissions = listOf(OpenBusinessPermissions.AiOnCall),
                sortOrder = 70
            )
        )
    }

    private fun targetTab(
        id: String,
        displayName: String,
        description: String,
        icon: String,
        route: String,
        permissions: List<String>,
        sortOrder: Int
    ): TabManifest {
        return TabManifest(
            id = id,
            displayName = displayName,
            description = description,
            icon = icon,
            route = route,
            entryType = EntryType.Native,
            entryUri = null,
            version = SemanticVersionDto(major = 1, minor = 0, patch = 0),
            minContainerVersion = 1,
            permissions = permissions,
            enabled = true,
            sortOrder = sortOrder,
            extension = TabExtensionDto(
                titleBar = TitleBarExtensionDto(rightText = null, menuItems = emptyList()),
                fab = null
            ),
            extraConfig = emptyMap()
        )
    }

    private fun RegisteredOpenTab.toManifest(sortOrder: Int): TabManifest {
        val definition = definition
        return TabManifest(
            id = definition.id,
            displayName = definition.displayName,
            description = "通过客户端协议注册的业务 Tab。",
            icon = definition.id,
            route = definition.route,
            entryType = EntryType.Native,
            entryUri = null,
            version = SemanticVersionDto(
                major = definition.version.major,
                minor = definition.version.minor,
                patch = definition.version.patch
            ),
            minContainerVersion = definition.minContainerVersion,
            permissions = definition.permissions,
            enabled = true,
            sortOrder = sortOrder,
            extension = TabExtensionDto(
                titleBar = definition.extension?.titleBar?.let { titleBar ->
                    TitleBarExtensionDto(
                        rightText = titleBar.rightText,
                        menuItems = titleBar.menuItems.map { item ->
                            MenuItemDto(id = item.id, label = item.label)
                        }
                    )
                },
                fab = definition.extension?.fab?.let { fab ->
                    FabExtensionDto(
                        id = "protocol-${definition.id}-fab",
                        icon = null,
                        label = fab.label
                    )
                }
            ),
            extraConfig = mapOf("source" to "protocol")
        )
    }

    private companion object {

        const val CLIENT_BUILT_IN_AI_ONCALL = "ai-oncall"
        const val PROTOCOL_REGISTERED_SORT_ORDER = 80

    }

}

private fun OpenApiResult<SuccessResponse>.requireSuccessFlag(defaultMessage: String): OpenApiResult<SuccessResponse> {
    return when (this) {
        is OpenApiResult.Success -> {
            if (data.success) {
                this
            } else {
                OpenApiResult.Failed(code = "BUSINESS_FAILED", message = defaultMessage)
            }
        }

        is OpenApiResult.Failed -> this
    }
}

private fun TabVisibilityRequest.toJson(): JSONObject {
    return JSONObject()
        .put("scope", scope)
        .put("teamIds", JSONArray(teamIds))
        .put("userIds", JSONArray(userIds))
        .put("defaultEnabled", defaultEnabled)
}
