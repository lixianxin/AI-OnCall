package github.leavesczy.compose_chat.ui

import android.graphics.Color as AndroidColor
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.ui.OpenOnCallHomePage
import github.leavesczy.compose_chat.open.ui.OpenOnCallPage
import github.leavesczy.compose_chat.open.ui.OpenProfilePage
import github.leavesczy.compose_chat.open.ui.OpenTabContentHost
import github.leavesczy.compose_chat.open.ui.OpenWorkbenchPage
import github.leavesczy.compose_chat.provider.AppThemeProvider
import github.leavesczy.compose_chat.ui.conversation.ConversationPage
import github.leavesczy.compose_chat.ui.conversation.logic.ConversationViewModel
import github.leavesczy.compose_chat.ui.friendship.FriendshipDialog
import github.leavesczy.compose_chat.ui.friendship.FriendshipPage
import github.leavesczy.compose_chat.ui.friendship.logic.FriendshipViewModel
import github.leavesczy.compose_chat.ui.logic.AppTheme as UserAppTheme
import github.leavesczy.compose_chat.ui.logic.MainPageTab
import github.leavesczy.compose_chat.ui.logic.MainViewModel
import github.leavesczy.compose_chat.ui.person.logic.PersonProfileViewModel
import github.leavesczy.compose_chat.ui.theme.AppTheme

/**
 * @Author: leavesCZY
 * @Date: 2026/5/20 17:18
 * @Desc:
 */
@Composable
fun MainPage(
    mainViewModel: MainViewModel,
    conversationViewModel: ConversationViewModel,
    friendshipViewModel: FriendshipViewModel,
    personProfileViewModel: PersonProfileViewModel
) {
    val bottomBarViewState = mainViewModel.bottomBarViewState
    val selectedOpenTab = bottomBarViewState.openTabs.firstOrNull { tab ->
        tab.id == bottomBarViewState.selectedOpenTabId
    }
    val inWebBusinessTab = bottomBarViewState.selectedTab == MainPageTab.Workbench &&
        selectedOpenTab?.manifest?.entryType == EntryType.Web
    MainPageSystemBarEffect(inWebBusinessTab = inWebBusinessTab)
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        contentWindowInsets = WindowInsets(),
        containerColor = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color,
        bottomBar = {
            val inOnCallChat = bottomBarViewState.selectedTab == MainPageTab.AiOncall &&
                bottomBarViewState.selectedOpenTabId == "ai-oncall"
            if (!inOnCallChat && !inWebBusinessTab) {
                MainPageBottomBar(viewState = mainViewModel.bottomBarViewState)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (bottomBarViewState.selectedTab == MainPageTab.AiOncall &&
                bottomBarViewState.selectedOpenTabId == "ai-oncall"
            ) {
                key(bottomBarViewState.onCallRouteKey) {
                    OpenOnCallPage(
                        initialSessionId = bottomBarViewState.onCallSessionId,
                        initialSessionTitle = bottomBarViewState.onCallSessionTitle,
                        initialPrompt = bottomBarViewState.onCallInitialPrompt,
                        forceNewSession = bottomBarViewState.onCallForceNewSession,
                        onBackToOnCallHome = mainViewModel::backToOnCallHome
                    )
                }
            } else if (bottomBarViewState.selectedTab == MainPageTab.Workbench && selectedOpenTab != null) {
                OpenTabContentHost(
                    tab = selectedOpenTab,
                    onBackToWorkbench = mainViewModel::backToWorkbench
                )
            } else {
                when (bottomBarViewState.selectedTab) {
                    MainPageTab.Conversation -> {
                        ConversationPage(pageViewState = conversationViewModel.pageViewState)
                    }

                    MainPageTab.Friendship -> {
                        FriendshipPage(pageViewState = friendshipViewModel.pageViewState)
                    }

                    MainPageTab.Workbench -> {
                        OpenWorkbenchPage(
                            openTabs = bottomBarViewState.openTabs,
                            onRefreshTabs = mainViewModel::refreshOpenTabs,
                            onClickOpenTab = bottomBarViewState.onClickOpenTab
                        )
                    }

                    MainPageTab.AiOncall -> {
                        OpenOnCallHomePage(
                            onOpenChat = mainViewModel::openOnCallChat
                        )
                    }

                    MainPageTab.Person -> {
                        OpenProfilePage(viewState = mainViewModel.openProfileViewState)
                    }
                }
            }
        }
    }
    FriendshipDialog(viewState = friendshipViewModel.friendshipDialogViewState)
}

@Composable
private fun MainPageSystemBarEffect(inWebBusinessTab: Boolean) {
    val localActivity = LocalActivity.current ?: return
    val appTheme = AppThemeProvider.appTheme
    SideEffect {
        val systemBarColor = if (inWebBusinessTab) {
            AndroidColor.rgb(246, 248, 251)
        } else {
            when (appTheme) {
                UserAppTheme.Light, UserAppTheme.Gray -> AndroidColor.rgb(246, 248, 251)
                UserAppTheme.Dark -> AndroidColor.rgb(16, 16, 16)
            }
        }
        val systemBarsDarkIcon = inWebBusinessTab || appTheme != UserAppTheme.Dark
        val window = localActivity.window
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor
        window.decorView.setBackgroundColor(systemBarColor)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            isAppearanceLightStatusBars = systemBarsDarkIcon
            isAppearanceLightNavigationBars = systemBarsDarkIcon
        }
    }
}
