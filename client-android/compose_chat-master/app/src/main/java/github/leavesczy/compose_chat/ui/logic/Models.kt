package github.leavesczy.compose_chat.ui.logic

import androidx.compose.runtime.Stable
import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.open.model.DebugStatusResponse
import github.leavesczy.compose_chat.open.model.MeResponse
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabState
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * @Author: leavesCZY
 * @Date: 2026/5/20 17:18
 * @Desc:
 */
@Stable
enum class MainPageTab {
    Conversation,
    Friendship,
    Workbench,
    AiOncall,
    Person;
}

@Stable
data class MainPageBottomBarViewState(
    val selectedTab: MainPageTab,
    val selectedOpenTabId: String?,
    val openTabs: List<OpenTabItem>,
    val onCallRouteKey: Long,
    val onCallSessionId: String?,
    val onCallSessionTitle: String?,
    val onCallInitialPrompt: String?,
    val onCallForceNewSession: Boolean,
    val unreadMessageCount: Long,
    val onClickTab: (tab: MainPageTab) -> Unit,
    val onClickOpenTab: (tab: OpenTabItem) -> Unit
)

@Stable
data class OpenProfileViewState(
    val loading: Boolean,
    val displayName: String,
    val userId: String,
    val teamName: String,
    val roleName: String,
    val permissions: List<String>,
    val serviceStatus: String,
    val serviceMode: String,
    val serverTime: String,
    val apiVersion: String,
    val sseAvailableText: String,
    val tabCount: Int,
    val openableTabCount: Int,
    val restrictedTabCount: Int,
    val baseUrl: String,
    val maskedToken: String,
    val containerVersion: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val refresh: () -> Unit,
    val logout: () -> Unit
)

fun buildOpenProfileViewState(
    loading: Boolean,
    me: MeResponse?,
    debugStatus: DebugStatusResponse?,
    openTabs: List<OpenTabItem>,
    errorCode: String?,
    errorMessage: String?,
    refresh: () -> Unit,
    logout: () -> Unit
): OpenProfileViewState {
    val openableTabCount = openTabs.count { tab ->
        tab.openState == OpenTabState.Openable
    }
    return OpenProfileViewState(
        loading = loading,
        displayName = me?.displayName ?: OpenSessionManager.displayName.ifBlank { "训练营用户" },
        userId = me?.userId ?: OpenSessionManager.userId.ifBlank { "-" },
        teamName = me?.currentTeamName() ?: me?.team?.name ?: "未加入团队",
        roleName = me?.roleName() ?: "普通账号",
        permissions = me?.permissions ?: OpenSessionManager.permissions.toList().sorted(),
        serviceStatus = debugStatus?.let { "已连接" } ?: "待刷新",
        serviceMode = debugStatus?.let { if (it.mockMode) "Mock 模式" else "正式模式" } ?: "-",
        serverTime = debugStatus?.serverTime.toProfileTimeText(),
        apiVersion = debugStatus?.apiVersion ?: "-",
        sseAvailableText = debugStatus?.let { if (it.sseAvailable) "可用" else "不可用" } ?: "-",
        tabCount = openTabs.size,
        openableTabCount = openableTabCount,
        restrictedTabCount = openTabs.size - openableTabCount,
        baseUrl = OpenApiConfig.DEFAULT_BASE_URL,
        maskedToken = OpenSessionManager.token.maskToken(),
        containerVersion = OpenApiConfig.CONTAINER_VERSION,
        errorCode = errorCode,
        errorMessage = errorMessage,
        refresh = refresh,
        logout = logout
    )
}

private fun MeResponse.currentTeamName(): String? {
    return memberships.firstOrNull { membership -> membership.teamId == currentTeamId }?.teamName
}

private fun MeResponse.roleName(): String {
    if (globalRole == "admin") {
        return "系统管理员"
    }
    val role = memberships.firstOrNull { membership -> membership.teamId == currentTeamId }?.teamRole
        ?: memberships.firstOrNull()?.teamRole
    return when (role) {
        "manager" -> "部门主管"
        "employee" -> "普通员工"
        else -> "未分配角色"
    }
}

private fun String.maskToken(): String {
    if (isBlank()) {
        return "-"
    }
    if (length <= 8) {
        return "***"
    }
    return "${take(4)}***${takeLast(4)}"
}

private fun String?.toProfileTimeText(): String {
    if (isNullOrBlank()) {
        return "-"
    }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zoneId = ZoneId.of("Asia/Shanghai")
    return runCatching {
        OffsetDateTime.parse(this).atZoneSameInstant(zoneId).format(formatter)
    }.recoverCatching {
        LocalDateTime.parse(this).format(formatter)
    }.getOrElse {
        this
    }
}

@Stable
enum class AppTheme {
    Light,
    Dark,
    Gray;
}
