package github.leavesczy.compose_chat.open.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.R
import github.leavesczy.compose_chat.open.repository.OpenAvatarRepository
import github.leavesczy.compose_chat.ui.logic.OpenProfileViewState
import github.leavesczy.compose_chat.ui.widgets.ComponentImage
import github.leavesczy.compose_chat.ui.widgets.CommonButton

@Composable
fun OpenProfilePage(
    modifier: Modifier = Modifier,
    viewState: OpenProfileViewState
) {
    val context = LocalContext.current
    val avatarRepository = remember { OpenAvatarRepository(context = context) }
    var localAvatarPath by remember { mutableStateOf(avatarRepository.getLocalAvatarPath()) }
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            localAvatarPath = avatarRepository.saveAvatarFromUri(uri = uri)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(color = 0xFFF6F8FB))
            .statusBarsPadding()
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ProfileHeader(
            viewState = viewState,
            localAvatarPath = localAvatarPath,
            onClickAvatar = {
                avatarPickerLauncher.launch(
                    PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
        if (viewState.errorMessage != null) {
            ErrorCard(viewState = viewState)
        }
        AccountSummaryCard(viewState = viewState)
        ActionCard(viewState = viewState)
    }
}

@Composable
private fun ProfileHeader(
    viewState: OpenProfileViewState,
    localAvatarPath: String?,
    onClickAvatar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 18.dp))
            .background(color = Color.White)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(
            modifier = Modifier
                .size(size = 72.dp),
            localAvatarPath = localAvatarPath,
            onClick = onClickAvatar
        )
        Column(
            modifier = Modifier
                .weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = viewState.realDisplayName(),
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = "用户 ID：${viewState.userId}",
                fontSize = 14.sp,
                lineHeight = 17.sp,
                color = Color(color = 0xFF6B7280)
            )
            TeamBadge(teamName = viewState.teamName)
        }
    }
}

@Composable
private fun ProfileAvatar(
    modifier: Modifier,
    localAvatarPath: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape = CircleShape)
            .background(color = Color(color = 0xFFEFF6FF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ComponentImage(
            modifier = Modifier.fillMaxSize(),
            model = localAvatarPath ?: R.drawable.open_default_avatar,
            backgroundColor = Color(color = 0xFFEFF6FF)
        )
        Text(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .fillMaxWidth()
                .background(color = Color(color = 0xAA111827))
                .padding(vertical = 3.dp),
            text = "更换",
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = Color.White
        )
    }
}

@Composable
private fun TeamBadge(teamName: String) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 999.dp))
            .background(color = Color(color = 0xFFEFF6FF))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Business,
            contentDescription = null,
            tint = Color(color = 0xFF2563EB)
        )
        Text(
            text = teamName,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF1D4ED8)
        )
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
                tint = Color(color = 0xFF2563EB)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(space = 6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = viewState.errorMessage.orEmpty(),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = Color(color = 0xFF111827)
                )
                Text(
                    text = "错误码：${viewState.errorCode ?: "-"}",
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = Color(color = 0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(viewState: OpenProfileViewState) {
    SectionCard(title = "账号状态") {
        InfoRow(label = "服务连接", value = viewState.serviceStatus, iconSuccess = true)
        InfoRow(label = "团队", value = viewState.teamName)
        InfoRow(label = "角色", value = viewState.roleName)
        if (viewState.serverTime != "-") {
            InfoRow(label = "同步时间", value = viewState.serverTime)
        }
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
    val displayPermissions = permissions
        .map { permission -> permission.toDisplayPermissionName() }
        .distinct()
    SectionCard(title = "可用能力") {
        if (displayPermissions.isEmpty()) {
            Text(
                text = "当前账号暂无可用业务能力。",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Color(color = 0xFF6B7280)
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                displayPermissions.forEach { permission ->
                    PermissionChip(permission = permission)
                }
            }
        }
    }
}

@Composable
private fun CollapsibleDebugInfoCard(viewState: OpenProfileViewState) {
    var expanded by remember { mutableStateOf(false) }
    SectionCard(title = "调试信息") {
        Text(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = Color(color = 0xFFF3F6FA))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            text = if (expanded) "收起联调信息" else "展开联调信息",
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF2563EB)
        )
        if (expanded) {
            InfoRow(label = "Base URL", value = viewState.baseUrl)
            InfoRow(label = "Token", value = viewState.maskedToken)
            InfoRow(label = "容器版本", value = viewState.containerVersion.toString())
            InfoRow(label = "API 版本", value = viewState.apiVersion)
            InfoRow(label = "运行模式", value = viewState.serviceMode)
            InfoRow(label = "SSE", value = viewState.sseAvailableText)
        }
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
            .clip(shape = RoundedCornerShape(size = 16.dp))
            .background(color = Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
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
                tint = Color(color = 0xFF2563EB)
            )
        }
        Text(
            modifier = Modifier
                .weight(weight = 1f),
            text = label,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = Color(color = 0xFF6B7280)
        )
        Text(
            modifier = Modifier
                .weight(weight = 2f),
            text = value,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(color = 0xFF111827)
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
                .background(color = Color(color = 0xFFF3F6FA))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF2563EB)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = Color(color = 0xFF6B7280)
        )
    }
}

@Composable
private fun PermissionChip(permission: String) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 999.dp))
            .background(color = Color(color = 0xFFEFF6FF))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier,
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = Color(color = 0xFF2563EB)
        )
        Column {
            Text(
                text = permission,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
        }
    }
}

private fun String.toDisplayPermissionName(): String {
    return when (this) {
        "team.manage" -> "团队管理"
        "team.all.read" -> "全部团队"
        "team.member.read" -> "团队成员"
        "tab.company.read" -> "公司介绍"
        "tab.announcement.read" -> "公告"
        "tab.announcement.write" -> "公告发布"
        "tab.fun.read" -> "放松一刻"
        "tab.approval.read" -> "审批"
        "tab.approval.create" -> "发起审批"
        "tab.approval.approve" -> "处理审批"
        "tab.approval.all" -> "全部审批"
        "tab.calendar.read" -> "日程"
        "tab.calendar.create" -> "创建日程"
        "tab.calendar.manage" -> "管理日程"
        "tab.calendar.all" -> "全部日程"
        "tab.admin.manage" -> "权限管理"
        "tab.debug.read" -> "调试"
        "tab.finance.read" -> "财务"
        "ai.oncall" -> "AI oncall"
        else -> "业务权限"
    }
}

private fun OpenProfileViewState.realDisplayName(): String {
    val seedName = userId.toDemoRealNameByUserId()
    return when {
        seedName != null && displayName.isSeedRoleName() -> seedName
        seedName != null && displayName.isBlank() -> seedName
        displayName.isNotBlank() -> displayName
        seedName != null -> seedName
        else -> "未命名用户"
    }
}

private fun String.isSeedRoleName(): Boolean {
    return this == "产品主管" ||
        this == "产品员工" ||
        this == "运营主管" ||
        this == "运营员工" ||
        this == "部门主管" ||
        this == "普通员工" ||
        this == "演示账号" ||
        this == "系统管理员" ||
        this == "训练营用户"
}

private fun String.toDemoRealNameByUserId(): String? {
    return when (this) {
        "user-admin" -> "陈明"
        "user-demo" -> "林一凡"
        "user-guest" -> "访客用户"
        "user-product-manager" -> "王睿"
        "user-product-employee" -> "张晨"
        "user-operation-manager" -> "赵宁"
        "user-operation-employee" -> "李晓"
        else -> null
    }
}
