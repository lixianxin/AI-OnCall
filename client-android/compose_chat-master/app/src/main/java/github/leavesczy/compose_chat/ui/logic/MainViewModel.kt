package github.leavesczy.compose_chat.ui.logic

import android.content.Intent
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import github.leavesczy.compose_chat.base.models.PersonProfile
import github.leavesczy.compose_chat.base.models.ServerConnectState
import github.leavesczy.compose_chat.base.provider.IConversationProvider
import github.leavesczy.compose_chat.open.model.DebugStatusResponse
import github.leavesczy.compose_chat.open.model.MeResponse
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OpenAuthRepository
import github.leavesczy.compose_chat.open.repository.OpenTabRepository
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.provider.AccountProvider
import github.leavesczy.compose_chat.provider.AppThemeProvider
import github.leavesczy.compose_chat.proxy.ConversationProvider
import github.leavesczy.compose_chat.ui.base.BaseViewModel
import github.leavesczy.compose_chat.ui.login.LoginActivity
import github.leavesczy.compose_chat.ui.preview.PreviewImageActivity
import github.leavesczy.compose_chat.ui.profile.ProfileUpdateActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * @Author: leavesCZY
 * @Date: 2026/5/20 17:18
 * @Desc:
 */
class MainViewModel : BaseViewModel() {

    private val conversationProvider: IConversationProvider = ConversationProvider()

    private val openAuthRepository = OpenAuthRepository()

    private val openTabRepository = OpenTabRepository()

    private var openMe: MeResponse? = null

    private var openDebugStatus: DebugStatusResponse? = null

    private val _serverConnectState = MutableStateFlow(value = ServerConnectState.Connected)

    val serverConnectState: SharedFlow<ServerConnectState> = _serverConnectState

    val topBarViewState = MainPageTopBarViewState(
        openDrawer = ::openDrawer
    )

    var bottomBarViewState by mutableStateOf(
        value = MainPageBottomBarViewState(
            selectedTab = MainPageTab.Conversation,
            selectedOpenTabId = null,
            openTabs = emptyList(),
            unreadMessageCount = 0L,
            onClickTab = ::onClickTab,
            onClickOpenTab = ::onClickOpenTab
        )
    )
        private set

    var drawerViewState by mutableStateOf(
        value = MainPageDrawerViewState(
            drawerState = DrawerState(initialValue = DrawerValue.Closed),
            appTheme = AppThemeProvider.appTheme,
            personProfile = ComposeChat.accountProvider.personProfileFlow.value,
            previewImage = ::previewImage,
            switchTheme = ::switchTheme,
            logout = ::logout,
            updateProfile = ::updateProfile
        )
    )
        private set

    var openProfileViewState by mutableStateOf(
        value = buildOpenProfileViewState(
            loading = false,
            me = null,
            debugStatus = null,
            openTabs = emptyList(),
            errorCode = null,
            errorMessage = null,
            refresh = ::refreshOpenProfile,
            logout = ::logout
        )
    )
        private set

    init {
        loadOpenTabs()
        refreshOpenProfile()
        viewModelScope.launch {
            launch {
                requestData()
            }
            launch {
                conversationProvider.totalUnreadMessageCountFlow.collect { unreadMessageCount ->
                    onUnreadMessageCountChanged(unreadMessageCount = unreadMessageCount)
                }
            }
            launch {
                ComposeChat.accountProvider.personProfileFlow.collect { personProfile ->
                    onPersonProfileChanged(personProfile = personProfile)
                }
            }
            launch {
                ComposeChat.accountProvider.serverConnectStateFlow.collect { state ->
                    _serverConnectState.emit(value = state)
                    if (state == ServerConnectState.Connected) {
                        requestData()
                    }
                }
            }
        }
    }

    private suspend fun requestData() {
        // 训练营主登录已经切到服务端 token；腾讯 IM 仍作为原 demo 能力保留。
        // 当用户未登录 IM 时，这里的刷新可能失败，因此只做保护性调用，避免影响工作台和业务 Tab。
        runCatching {
            conversationProvider.refreshTotalUnreadMessageCount()
            ComposeChat.accountProvider.refreshPersonProfile()
        }
    }

    private fun onClickTab(mainPageTab: MainPageTab) {
        val viewState = bottomBarViewState
        if (mainPageTab == MainPageTab.AiOncall) {
            val oncallTab = viewState.openTabs.firstOrNull { tab ->
                tab.id == OPEN_TAB_AI_ONCALL
            }
            bottomBarViewState = viewState.copy(
                selectedTab = mainPageTab,
                selectedOpenTabId = oncallTab?.id
            )
            return
        }
        if (viewState.selectedTab != mainPageTab || viewState.selectedOpenTabId != null) {
            bottomBarViewState = viewState.copy(
                selectedTab = mainPageTab,
                selectedOpenTabId = null
            )
        }
    }

    private fun onClickOpenTab(openTabItem: OpenTabItem) {
        val viewState = bottomBarViewState
        if (viewState.selectedOpenTabId != openTabItem.id) {
            bottomBarViewState = viewState.copy(
                selectedTab = if (openTabItem.id == OPEN_TAB_AI_ONCALL) {
                    MainPageTab.AiOncall
                } else {
                    MainPageTab.Workbench
                },
                selectedOpenTabId = openTabItem.id
            )
        }
    }

    private fun loadOpenTabs() {
        viewModelScope.launch {
            val remoteTabs = openTabRepository.getRemoteTabs()
            val openTabs = when (remoteTabs) {
                is OpenApiResult.Success -> {
                    openTabRepository.buildItems(manifests = remoteTabs.data)
                }

                is OpenApiResult.Failed -> {
                    showToast(msg = "服务端 Tab 加载失败，已使用本地演示数据：${remoteTabs.message}")
                    openTabRepository.getMockTabs()
                }
            }
            bottomBarViewState = bottomBarViewState.copy(openTabs = openTabs)
            updateOpenProfileState()
        }
    }

    private fun refreshOpenProfile() {
        viewModelScope.launch {
            updateOpenProfileState(loading = true, errorCode = null, errorMessage = null)
            val meResult = openAuthRepository.me()
            val debugResult = openAuthRepository.debugStatus()
            var errorCode: String? = null
            var errorMessage: String? = null

            when (meResult) {
                is OpenApiResult.Success -> {
                    openMe = meResult.data
                }

                is OpenApiResult.Failed -> {
                    errorCode = meResult.code
                    errorMessage = "用户信息加载失败：${meResult.message}"
                }
            }

            when (debugResult) {
                is OpenApiResult.Success -> {
                    openDebugStatus = debugResult.data
                }

                is OpenApiResult.Failed -> {
                    errorCode = errorCode ?: debugResult.code
                    errorMessage = errorMessage ?: "服务状态加载失败：${debugResult.message}"
                }
            }
            updateOpenProfileState(
                loading = false,
                errorCode = errorCode,
                errorMessage = errorMessage
            )
        }
    }

    private fun updateOpenProfileState(
        loading: Boolean = openProfileViewState.loading,
        errorCode: String? = openProfileViewState.errorCode,
        errorMessage: String? = openProfileViewState.errorMessage
    ) {
        openProfileViewState = buildOpenProfileViewState(
            loading = loading,
            me = openMe,
            debugStatus = openDebugStatus,
            openTabs = bottomBarViewState.openTabs,
            errorCode = errorCode,
            errorMessage = errorMessage,
            refresh = ::refreshOpenProfile,
            logout = ::logout
        )
    }

    private fun onUnreadMessageCountChanged(unreadMessageCount: Long) {
        val viewState = bottomBarViewState
        if (viewState.unreadMessageCount != unreadMessageCount) {
            bottomBarViewState = viewState.copy(unreadMessageCount = unreadMessageCount)
        }
    }

    private fun onPersonProfileChanged(personProfile: PersonProfile) {
        val viewState = drawerViewState
        if (drawerViewState.personProfile != personProfile) {
            drawerViewState = viewState.copy(personProfile = personProfile)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            showLoadingDialog()
            // 当前主登录态是训练营服务端 token。退出时先清理服务端 session；
            // 腾讯 IM 仍作为原 demo 兼容能力，尝试退出但不让它阻塞主流程。
            OpenSessionManager.clear()
            AccountProvider.onUserLogout()
            runCatching {
                ComposeChat.accountProvider.logout()
            }.onFailure { error ->
                showToast(msg = error.message)
            }
            dismissLoadingDialog()
            navToLoginPage()
        }
    }

    private fun navToLoginPage() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private suspend fun openDrawer() {
        drawerViewState.drawerState.open()
    }

    private fun updateProfile() {
        context.startActivity<ProfileUpdateActivity>()
    }

    private fun previewImage(imageUrl: String) {
        if (imageUrl.isNotBlank()) {
            PreviewImageActivity.navTo(context = context, imageUri = imageUrl)
        }
    }

    private fun switchTheme() {
        val nextTheme = AppThemeProvider.appTheme.nextTheme()
        drawerViewState = drawerViewState.copy(appTheme = nextTheme)
        AppThemeProvider.onAppThemeChanged(appTheme = nextTheme)
    }

    private fun AppTheme.nextTheme(): AppTheme {
        val values = AppTheme.entries
        return values.getOrElse(ordinal + 1) {
            values[0]
        }
    }

    private companion object {

        const val OPEN_TAB_AI_ONCALL = "ai-oncall"

    }

}
