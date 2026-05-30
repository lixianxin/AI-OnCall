package github.leavesczy.compose_chat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import github.leavesczy.compose_chat.open.ui.OpenProfilePage
import github.leavesczy.compose_chat.open.ui.OpenTabContentHost
import github.leavesczy.compose_chat.open.ui.OpenWorkbenchPage
import github.leavesczy.compose_chat.ui.conversation.ConversationPage
import github.leavesczy.compose_chat.ui.conversation.logic.ConversationViewModel
import github.leavesczy.compose_chat.ui.friendship.FriendshipDialog
import github.leavesczy.compose_chat.ui.friendship.FriendshipPage
import github.leavesczy.compose_chat.ui.friendship.logic.FriendshipViewModel
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
    ModalNavigationDrawer(
        modifier = Modifier
            .fillMaxSize(),
        drawerState = mainViewModel.drawerViewState.drawerState,
        drawerContent = {
            MainPageDrawer(viewState = mainViewModel.drawerViewState)
        },
        content = {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
                contentWindowInsets = WindowInsets(),
                containerColor = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color,
                topBar = {
                    if (mainViewModel.bottomBarViewState.selectedOpenTabId != null || mainViewModel.bottomBarViewState.selectedTab != MainPageTab.Person) {
                        MainPageTopBar(
                            viewState = mainViewModel.topBarViewState,
                            showFriendshipDialog = {
                                friendshipViewModel.showFriendshipDialog()
                            }
                        )
                    }
                },
                bottomBar = {
                    MainPageBottomBar(viewState = mainViewModel.bottomBarViewState)
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues = innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val bottomBarViewState = mainViewModel.bottomBarViewState
                    val selectedOpenTab = bottomBarViewState.openTabs.firstOrNull { tab ->
                        tab.id == bottomBarViewState.selectedOpenTabId
                    }
                    if (selectedOpenTab != null) {
                        OpenTabContentHost(tab = selectedOpenTab)
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
                                    onClickOpenTab = bottomBarViewState.onClickOpenTab
                                )
                            }

                            MainPageTab.AiOncall -> {
                                OpenTabContentHost(tab = null)
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
    )
}
