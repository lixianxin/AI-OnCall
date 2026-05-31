package github.leavesczy.compose_chat.open.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.ui.theme.AppTheme

@Composable
fun OpenWorkbenchPage(
    modifier: Modifier = Modifier,
    openTabs: List<OpenTabItem>,
    onClickOpenTab: (OpenTabItem) -> Unit
) {
    val businessTabs = openTabs.filterNot { tab ->
        tab.id == "ai-oncall"
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "工作台",
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            text = "业务能力统一收纳在工作台，底部只保留高频入口。",
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        businessTabs.chunked(size = 2).forEach { rowTabs ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                rowTabs.forEach { tab ->
                    WorkbenchItem(
                        modifier = Modifier
                            .weight(weight = 1f),
                        tab = tab,
                        onClick = {
                            onClickOpenTab(tab)
                        }
                    )
                }
                if (rowTabs.size == 1) {
                    Column(
                        modifier = Modifier
                            .weight(weight = 1f)
                    ) {
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkbenchItem(
    modifier: Modifier,
    tab: OpenTabItem,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(
            modifier = Modifier
                .size(size = 24.dp),
            imageVector = tab.icon,
            contentDescription = null,
            tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
        Text(
            text = tab.displayName,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            text = tab.openState.toWorkbenchStateText(),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

private fun OpenTabState.toWorkbenchStateText(): String {
    return when (this) {
        OpenTabState.Openable -> "可打开"
        OpenTabState.Disabled -> "已禁用"
        OpenTabState.PermissionDenied -> "权限不足"
        OpenTabState.VersionIncompatible -> "版本不兼容"
        OpenTabState.RouteUnsupported -> "路由不支持"
        OpenTabState.EntryUnsupported -> "入口不支持"
        OpenTabState.InvalidConfig -> "配置异常"
    }
}
