package github.leavesczy.compose_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.R
import github.leavesczy.compose_chat.ui.logic.MainPageBottomBarViewState
import github.leavesczy.compose_chat.ui.logic.MainPageTab
import github.leavesczy.compose_chat.ui.theme.AppTheme

/**
 * @Author: leavesCZY
 * @Date: 2026/5/20 17:18
 * @Desc:
 */
@Composable
fun MainPageBottomBar(viewState: MainPageBottomBarViewState) {
    val unreadCountOverflow = stringResource(id = R.string.unread_count_overflow)
    // 原 demo 的“消息 / 通讯录”逻辑继续保留，只是不再作为当前产品的底部主入口展示。
    val productTabs = listOf(
        MainPageTab.Workbench,
        MainPageTab.AiOncall,
        MainPageTab.Person
    )
    Row(
        modifier = Modifier
            .shadow(elevation = 28.dp)
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(height = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (pageTab in productTabs) {
            val icon: ImageVector
            val label: String
            val unreadMessageCount: Long
            when (pageTab) {
                MainPageTab.Conversation -> {
                    icon = Icons.Filled.Apps
                    label = "消息"
                    unreadMessageCount = viewState.unreadMessageCount
                }

                MainPageTab.Friendship -> {
                    icon = Icons.Filled.Apps
                    label = "通讯录"
                    unreadMessageCount = 0
                }

                MainPageTab.Workbench -> {
                    icon = Icons.Filled.Apps
                    label = "工作台"
                    unreadMessageCount = 0
                }

                MainPageTab.AiOncall -> {
                    icon = Icons.Filled.SmartToy
                    label = "AI助手"
                    unreadMessageCount = 0
                }

                MainPageTab.Person -> {
                    icon = Icons.Filled.Person
                    label = "我的"
                    unreadMessageCount = 0
                }
            }
            NavigationBarItem(
                icon = icon,
                label = label,
                selected = viewState.selectedTab == pageTab,
                unreadMessageCount = unreadMessageCount,
                unreadCountOverflow = unreadCountOverflow,
                onClick = {
                    viewState.onClickTab(pageTab)
                }
            )
        }
    }
}

@Composable
private fun RowScope.NavigationBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    unreadMessageCount: Long,
    unreadCountOverflow: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        modifier = Modifier
            .weight(weight = 1f),
        icon = {
            Box {
                Icon(
                    modifier = Modifier
                        .size(size = 22.dp),
                    imageVector = icon,
                    contentDescription = null
                )
                if (unreadMessageCount > 0) {
                    Text(
                        modifier = Modifier
                            .offset(x = 18.dp, y = (-10).dp)
                            .size(size = 22.dp)
                            .background(
                                color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
                                shape = CircleShape
                            )
                            .wrapContentSize(align = Alignment.Center),
                        text = if (unreadMessageCount > 99) {
                            unreadCountOverflow
                        } else {
                            unreadMessageCount.toString()
                        },
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        color = AppTheme.colorScheme.c_FFFFFFFF_FFFFFFFF.color
                    )
                }
            }
        },
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        },
        alwaysShowLabel = true,
        selected = selected,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
            selectedTextColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
            unselectedIconColor = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color,
            unselectedTextColor = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        ),
        onClick = onClick
    )
}
