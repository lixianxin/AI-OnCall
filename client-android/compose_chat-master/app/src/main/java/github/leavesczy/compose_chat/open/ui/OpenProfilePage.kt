package github.leavesczy.compose_chat.open.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.ui.logic.OpenProfileViewState
import github.leavesczy.compose_chat.ui.theme.AppTheme
import github.leavesczy.compose_chat.ui.widgets.CommonButton

@Composable
fun OpenProfilePage(
    modifier: Modifier = Modifier,
    viewState: OpenProfileViewState
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ProfileHeader(viewState = viewState)
        if (viewState.errorMessage != null) {
            ErrorCard(viewState = viewState)
        }
        ServiceStatusCard(viewState = viewState)
        BusinessSummaryCard(viewState = viewState)
        PermissionCard(permissions = viewState.permissions)
        DebugInfoCard(viewState = viewState)
        ActionCard(viewState = viewState)
    }
}

@Composable
private fun ProfileHeader(viewState: OpenProfileViewState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .clip(shape = CircleShape)
                .background(color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color)
                .padding(all = 14.dp),
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = AppTheme.colorScheme.c_FFFFFFFF_FFFFFFFF.color
        )
        Column(
            modifier = Modifier
                .weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = viewState.displayName,
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "用户 ID：${viewState.userId}",
                fontSize = 14.sp,
                lineHeight = 17.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
            Text(
                text = "团队：${viewState.teamName}",
                fontSize = 14.sp,
                lineHeight = 17.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
    }
}

@Composable
private fun ErrorCard(viewState: OpenProfileViewState) {
    SectionCard(title = "异常信息") {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(space = 6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = viewState.errorMessage.orEmpty(),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                Text(
                    text = "错误码：${viewState.errorCode ?: "-"}",
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(viewState: OpenProfileViewState) {
    SectionCard(title = "服务状态") {
        InfoRow(label = "连接状态", value = viewState.serviceStatus, iconSuccess = true)
        InfoRow(label = "运行模式", value = viewState.serviceMode)
        InfoRow(label = "API 版本", value = viewState.apiVersion)
        InfoRow(label = "SSE", value = viewState.sseAvailableText)
        InfoRow(label = "服务时间", value = viewState.serverTime)
    }
}

@Composable
private fun BusinessSummaryCard(viewState: OpenProfileViewState) {
    SectionCard(title = "业务能力") {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryItem(
                modifier = Modifier.weight(weight = 1f),
                label = "入口",
                value = viewState.tabCount.toString()
            )
            SummaryItem(
                modifier = Modifier.weight(weight = 1f),
                label = "可打开",
                value = viewState.openableTabCount.toString()
            )
            SummaryItem(
                modifier = Modifier.weight(weight = 1f),
                label = "受限",
                value = viewState.restrictedTabCount.toString()
            )
        }
    }
}

@Composable
private fun PermissionCard(permissions: List<String>) {
    SectionCard(title = "权限") {
        if (permissions.isEmpty()) {
            Text(
                text = "当前账号没有业务权限。",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                permissions.forEach { permission ->
                    PermissionChip(permission = permission)
                }
            }
        }
    }
}

@Composable
private fun DebugInfoCard(viewState: OpenProfileViewState) {
    SectionCard(title = "调试信息") {
        // token 只展示掩码，方便联调确认登录态存在，同时避免在界面上暴露完整凭证。
        InfoRow(label = "Base URL", value = viewState.baseUrl)
        InfoRow(label = "Token", value = viewState.maskedToken)
        InfoRow(label = "容器版本", value = viewState.containerVersion.toString())
    }
}

@Composable
private fun ActionCard(viewState: OpenProfileViewState) {
    SectionCard(title = "操作") {
        CommonButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = if (viewState.loading) "刷新中..." else "刷新信息",
            onClick = viewState.refresh
        )
        CommonButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            text = "退出登录",
            onClick = viewState.logout
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    iconSuccess: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconSuccess) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            )
        }
        Text(
            modifier = Modifier
                .weight(weight = 1f),
            text = label,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Text(
            modifier = Modifier
                .weight(weight = 2f),
            text = value,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
    }
}

@Composable
private fun SummaryItem(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

@Composable
private fun PermissionChip(permission: String) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier,
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
        Column {
            Text(
                text = permission.toDisplayPermissionName(),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = permission,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
    }
}

private fun String.toDisplayPermissionName(): String {
    return when (this) {
        "tab.approval.read" -> "审批"
        "tab.calendar.read" -> "日程"
        "tab.finance.read" -> "财务"
        "ai.oncall" -> "AI oncall"
        else -> "业务权限"
    }
}
