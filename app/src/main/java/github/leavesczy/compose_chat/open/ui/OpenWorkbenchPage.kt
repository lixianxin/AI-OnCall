package github.leavesczy.compose_chat.open.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.open.model.CreateCustomWebTabRequest
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.TabVisibilityRequest
import github.leavesczy.compose_chat.open.model.UpdateCustomWebTabRequest
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OpenTeamBusinessRepository
import github.leavesczy.compose_chat.open.repository.OpenTabRepository
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabRegistry
import github.leavesczy.compose_chat.open.tab.OpenTabSource
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.ui.theme.AppTheme
import github.leavesczy.compose_chat.ui.widgets.ComponentImage
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun OpenWorkbenchPage(
    modifier: Modifier = Modifier,
    openTabs: List<OpenTabItem>,
    onRefreshTabs: () -> Unit,
    onClickOpenTab: (OpenTabItem) -> Unit,
    repository: OpenTabRepository = remember { OpenTabRepository() }
) {
    var manageMode by remember { mutableStateOf(false) }
    val businessTabs = openTabs.filterNot { tab ->
        tab.isAiOnCallTab()
    }
    val visibleBusinessTabs = businessTabs.filter { tab ->
        tab.isEnabledOnWorkbench()
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
        if (manageMode) {
            TabManagePanel(
                openTabs = businessTabs,
                repository = repository,
                onRefreshTabs = onRefreshTabs,
                onBack = { manageMode = false }
            )
        } else {
            WorkbenchHeader(
                onClickManage = { manageMode = true },
                onRefresh = onRefreshTabs
            )
            WorkbenchHeroBanner()
            WorkbenchCategoryList(
                businessTabs = visibleBusinessTabs,
                onClickOpenTab = onClickOpenTab
            )
        }
    }
}

@Composable
private fun WorkbenchHeader(
    onClickManage: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                text = "工作台",
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = "团队应用与待办入口。",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Color(color = 0xFF6B7280)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            HeaderIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "刷新工作台",
                onClick = onRefresh
            )
            HeaderIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "管理业务应用",
                onClick = onClickManage
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier
            .size(size = 40.dp)
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = Color.White),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(size = 22.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(color = 0xFF2563EB)
        )
    }
}

@Composable
private fun WorkbenchHeroBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFEAF4FF))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 54.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 30.dp),
                imageVector = Icons.Rounded.Groups,
                contentDescription = null,
                tint = Color(color = 0xFF2563EB)
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "AI-OnCall 企业工作台",
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = "让审批、日程、公告和智能协作在一个入口完成。",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = Color(color = 0xFF37546D)
            )
        }
    }
}

@Composable
private fun WorkbenchCategoryList(
    businessTabs: List<OpenTabItem>,
    onClickOpenTab: (OpenTabItem) -> Unit
) {
    if (businessTabs.isEmpty()) {
        InfoBanner(text = "当前还没有可展示的业务应用，请刷新或进入管理模式添加入口。")
        return
    }
    val categories = WorkbenchCategories.mapNotNull { category ->
        val tabs = businessTabs.filter { tab -> category.matches(tab = tab) }
        if (tabs.isEmpty()) null else category to tabs
    }
    val categorizedIds = categories.flatMap { (_, tabs) -> tabs.map { tab -> tab.id } }.toSet()
    val customTabs = businessTabs.filterNot { tab -> tab.id in categorizedIds }
    (categories + listOfNotNull(customTabs.takeIf { it.isNotEmpty() }?.let { WorkbenchCustomCategory to it })).forEach { (category, tabs) ->
        WorkbenchCategorySection(
            category = category,
            tabs = tabs,
            onClickOpenTab = onClickOpenTab
        )
    }
}

@Composable
private fun WorkbenchCategorySection(
    category: WorkbenchCategory,
    tabs: List<OpenTabItem>,
    onClickOpenTab: (OpenTabItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color.White)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(size = 18.dp),
                imageVector = category.icon,
                contentDescription = null,
                tint = category.color
            )
            Text(
                text = category.title,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
        }
        tabs.chunked(size = 4).forEach { rowTabs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                rowTabs.forEach { tab ->
                    WorkbenchItem(
                        modifier = Modifier.weight(weight = 1f),
                        tab = tab,
                        accentColor = category.color,
                        onClick = { onClickOpenTab(tab) }
                    )
                }
                repeat(4 - rowTabs.size) {
                    Box(modifier = Modifier.weight(weight = 1f))
                }
            }
        }
    }
}

@Composable
private fun WorkbenchItem(
    modifier: Modifier,
    tab: OpenTabItem,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size = 44.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 24.dp),
                imageVector = tab.icon,
                contentDescription = null,
                tint = accentColor
            )
        }
        Text(
            text = tab.displayName,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
    }
}

@Composable
private fun TabManagePanel(
    openTabs: List<OpenTabItem>,
    repository: OpenTabRepository,
    onRefreshTabs: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val targetRepository = remember { OpenTeamBusinessRepository() }
    var catalog by remember { mutableStateOf<List<OpenTabItem>>(emptyList()) }
    var visibilityTargets by remember { mutableStateOf(DefaultVisibilityTargets) }
    var loadingCatalog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var pageMode by remember { mutableStateOf(TabManagePageMode.List) }
    var processingTabId by remember { mutableStateOf<String?>(null) }
    var managingTab by remember { mutableStateOf<OpenTabItem?>(null) }
    val canCreateBusinessApp = OpenSessionManager.permissions.contains("tab.admin.manage") ||
        OpenSessionManager.permissions.contains("team.manage")
    val enabledTabs = openTabs.filter { tab -> tab.isEnabledOnWorkbench() }
    val disabledTabs = openTabs.filterNot { tab -> tab.isEnabledOnWorkbench() }

    fun refreshCatalog() {
        loadingCatalog = true
        scope.launch {
            when (val result = repository.getCatalogTabs()) {
                is OpenApiResult.Success -> {
                    catalog = repository.buildItems(
                        manifests = result.data,
                        mergeTargetBusinessTabs = true
                    )
                    actionMessage = null
                }

                is OpenApiResult.Failed -> {
                    actionMessage = "目录加载失败：${result.message}"
                }
            }
            loadingCatalog = false
        }
    }

    fun refreshVisibilityTargets() {
        if (!canCreateBusinessApp) {
            return
        }
        scope.launch {
            when (val teamResult = targetRepository.teams()) {
                is OpenApiResult.Success -> {
                    val teamTargets = teamResult.data.map { team ->
                        VisibilityTarget(
                            id = team.teamId,
                            name = team.teamName,
                            description = team.description.ifBlank { "${team.memberCount} 人" }
                        )
                    }
                    val userTargets = linkedMapOf<String, VisibilityTarget>()
                    teamResult.data.forEach { team ->
                        when (val memberResult = targetRepository.teamMembers(teamId = team.teamId)) {
                            is OpenApiResult.Success -> {
                                memberResult.data.forEach { member ->
                                    userTargets[member.userId] = VisibilityTarget(
                                        id = member.userId,
                                        name = member.displayName,
                                        description = "${member.teamName} · ${member.account}"
                                    )
                                }
                            }

                            is OpenApiResult.Failed -> Unit
                        }
                    }
                    visibilityTargets = VisibilityTargets(
                        teamTargets = teamTargets.ifEmpty { DefaultVisibilityTargets.teamTargets },
                        userTargets = userTargets.values.toList().ifEmpty { DefaultVisibilityTargets.userTargets }
                    )
                }

                is OpenApiResult.Failed -> {
                    visibilityTargets = DefaultVisibilityTargets
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        refreshCatalog()
        refreshVisibilityTargets()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        ManagePageHeader(
            title = when (pageMode) {
                TabManagePageMode.List -> "业务管理"
                TabManagePageMode.ContainerType -> "新建业务"
                TabManagePageMode.CreateCustomWeb -> "Web 页面注册"
            },
            onBack = {
                when (pageMode) {
                    TabManagePageMode.List -> onBack()
                    TabManagePageMode.ContainerType -> pageMode = TabManagePageMode.List
                    TabManagePageMode.CreateCustomWeb -> pageMode = TabManagePageMode.ContainerType
                }
            }
        )
        if (actionMessage != null) {
            InfoBanner(text = actionMessage.orEmpty())
        }
        when (pageMode) {
            TabManagePageMode.ContainerType -> {
                ContainerTypeEntryList(
                    onSelectWeb = { pageMode = TabManagePageMode.CreateCustomWeb }
                )
            }

            TabManagePageMode.CreateCustomWeb -> {
                CustomWebTabForm(
                    repository = repository,
                    visibilityTargets = visibilityTargets,
                    onCancel = { pageMode = TabManagePageMode.ContainerType },
                    onCreated = {
                        actionMessage = "业务已发布，目标成员刷新工作台后可见"
                        pageMode = TabManagePageMode.List
                        onRefreshTabs()
                        refreshCatalog()
                    },
                    onMessage = { message ->
                        actionMessage = message
                    }
                )
            }

            TabManagePageMode.List -> {
                if (canCreateBusinessApp) {
                    CreateBusinessAppEntry(
                        onClick = { pageMode = TabManagePageMode.ContainerType }
                    )
                }
                SectionTitle(title = "已启用")
                if (enabledTabs.isEmpty()) {
                    InfoBanner(text = "当前工作台没有已启用的业务入口。")
                }
                enabledTabs.forEach { tab ->
                    val adminAction = canCreateBusinessApp
                    val canToggleWorkbench = tab.canUseServerWorkbenchMutation()
                    ManageTabRow(
                        tab = tab,
                        actionText = when {
                            processingTabId == tab.id -> "处理中"
                            adminAction -> "管理"
                            canToggleWorkbench -> "停用"
                            else -> "内置"
                        },
                        actionEnabled = !tab.isAiOnCallTab() &&
                            processingTabId == null &&
                            (adminAction || canToggleWorkbench),
                        onAction = {
                            if (adminAction) {
                                managingTab = tab
                            } else if (!canToggleWorkbench) {
                                actionMessage = "客户端内置或协议注册入口暂不支持服务端停用。"
                            } else {
                                scope.launch {
                                    processingTabId = tab.id
                                    when (val result = repository.disableTab(tabId = tab.id)) {
                                        is OpenApiResult.Success -> {
                                            actionMessage = "已停用 ${tab.displayName}"
                                            onRefreshTabs()
                                            refreshCatalog()
                                        }

                                        is OpenApiResult.Failed -> {
                                            actionMessage = if (result.code == "FORBIDDEN") {
                                                "权限不足，无法停用 ${tab.displayName}"
                                            } else {
                                                "已在本机停用 ${tab.displayName}，服务端同步稍后重试"
                                            }
                                            if (result.code != "FORBIDDEN") {
                                                onRefreshTabs()
                                                refreshCatalog()
                                            }
                                        }
                                    }
                                    processingTabId = null
                                }
                            }
                        }
                    )
                }
                SectionTitle(title = "可添加的业务应用")
                val enabledIds = enabledTabs.map { it.id }.toSet()
                val addableTabs = (disabledTabs + catalog)
                    .distinctBy { tab -> tab.id }
                    .filterNot { tab -> tab.id in enabledIds || tab.isAiOnCallTab() }
                if (loadingCatalog) {
                    InfoBanner(text = "正在加载业务应用目录…")
                }
                addableTabs.forEach { tab ->
                    val adminCanManageConfig = canCreateBusinessApp && tab.isEditableCustomWebTab()
                    val canToggleWorkbench = tab.canUseServerWorkbenchMutation()
                    ManageTabRow(
                        tab = tab,
                        actionText = when {
                            processingTabId == tab.id -> "处理中"
                            adminCanManageConfig -> "管理"
                            tab.openState == OpenTabState.PermissionDenied -> "无权限"
                            !canToggleWorkbench -> "内置"
                            else -> "启用"
                        },
                        actionEnabled = (adminCanManageConfig || (canToggleWorkbench &&
                            tab.openState != OpenTabState.PermissionDenied)) &&
                            processingTabId == null,
                        onAction = {
                            if (adminCanManageConfig) {
                                managingTab = tab
                                return@ManageTabRow
                            }
                            if (!canToggleWorkbench) {
                                actionMessage = "客户端内置或协议注册入口暂不支持服务端启用。"
                                return@ManageTabRow
                            }
                            scope.launch {
                                processingTabId = tab.id
                                when (val result = repository.enableTab(tabId = tab.id)) {
                                    is OpenApiResult.Success -> {
                                        actionMessage = "已启用 ${tab.displayName}"
                                        onRefreshTabs()
                                        refreshCatalog()
                                    }

                                    is OpenApiResult.Failed -> {
                                        actionMessage = when (result.code) {
                                            "FORBIDDEN" -> "已启用 ${tab.displayName}，打开时会进行权限校验"
                                            else -> "已在本机启用 ${tab.displayName}，服务端同步稍后重试"
                                        }
                                        onRefreshTabs()
                                        refreshCatalog()
                                    }
                                }
                                processingTabId = null
                            }
                        }
                    )
                }
            }
        }
        managingTab?.let { tab ->
            BusinessManageDialog(
                tab = tab,
                repository = repository,
                visibilityTargets = visibilityTargets,
                canEditConfig = tab.isEditableCustomWebTab(),
                canToggleWorkbench = tab.canUseServerWorkbenchMutation(),
                isEnabledOnWorkbench = tab.isEnabledOnWorkbench(),
                onDismiss = { managingTab = null },
                onChanged = { message ->
                    actionMessage = message
                    managingTab = null
                    onRefreshTabs()
                    refreshCatalog()
                }
            )
        }
    }
}

@Composable
private fun ManagePageHeader(
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        PlainBackButton(
            modifier = Modifier.align(alignment = Alignment.CenterStart),
            onClick = onBack
        )
        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
    }
}

@Composable
private fun PlainBackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(size = 38.dp)
            .clip(shape = RoundedCornerShape(size = 999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(size = 22.dp),
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回",
            tint = Color(color = 0xFF2563EB)
        )
    }
}

@Composable
private fun CreateBusinessAppEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFEAF4FF))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 38.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 22.dp),
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = Color(color = 0xFF2563EB)
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                text = "新建业务",
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = "选择接入类型，发布给指定成员或部门",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = Color(color = 0xFF37546D)
            )
        }
        Text(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color.White)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            text = "新建",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF2563EB)
        )
    }
}

@Composable
private fun ContainerTypeEntryList(onSelectWeb: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        BusinessContainerType.entries.forEach { type ->
            ContainerTypeEntryCard(
                type = type,
                onClick = {
                    if (type == BusinessContainerType.Web) {
                        onSelectWeb()
                    }
                }
            )
        }
    }
}

@Composable
private fun ContainerTypeEntryCard(
    type: BusinessContainerType,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = if (type.enabled) Color.White else Color(color = 0xFFEFF1F3))
            .clickable(enabled = type.enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 42.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = type.color.copy(alpha = if (type.enabled) 0.12f else 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 24.dp),
                imageVector = type.icon,
                contentDescription = null,
                tint = if (type.enabled) type.color else Color(color = 0xFF94A3B8)
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                text = "${type.title} · ${if (type.enabled) "已开放" else "待开放"}",
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = type.description,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = Color(color = 0xFF475569)
            )
        }
        if (type.enabled) {
            Icon(
                modifier = Modifier.size(size = 18.dp),
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = type.color
            )
        }
    }
}

@Composable
private fun ManageTabRow(
    tab: OpenTabItem,
    actionText: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    extraActions: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(size = 22.dp),
                imageVector = tab.icon,
                contentDescription = null,
                tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            )
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 3.dp)
            ) {
                Text(
                    text = tab.displayName,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                Text(
                    text = "${tab.manifest.entryType} · ${tab.openState.toWorkbenchStateText()}",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
            SmallActionButton(
                text = actionText,
                enabled = actionEnabled,
                onClick = onAction
            )
        }
        extraActions?.invoke()
    }
}

@Composable
private fun BusinessManageDialog(
    tab: OpenTabItem,
    repository: OpenTabRepository,
    visibilityTargets: VisibilityTargets,
    canEditConfig: Boolean,
    canToggleWorkbench: Boolean,
    isEnabledOnWorkbench: Boolean,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var processing by remember(tab.id) { mutableStateOf(false) }
    var localMessage by remember(tab.id) { mutableStateOf<String?>(null) }
    var deleteArmed by remember(tab.id) { mutableStateOf(false) }
    var editingVisibility by remember(tab.id) { mutableStateOf(false) }
    var visibilityDraft by remember(tab.id) {
        mutableStateOf(tab.manifest.visibility.toSelection())
    }

    AlertDialog(
        onDismissRequest = {
            if (!processing) {
                onDismiss()
            }
        },
        title = { Text(text = "管理 ${tab.displayName}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 10.dp)
            ) {
                InfoBanner(
                    text = "来源：${tab.source.toDisplayText()} · 状态：${tab.openState.toWorkbenchStateText()}"
                )
                InfoBanner(
                    text = "当前范围：${tab.manifest.visibility.toVisibilityText(visibilityTargets)}"
                )
                localMessage?.let { message ->
                    InfoBanner(text = message)
                }
                if (editingVisibility) {
                    VisibilitySelector(
                        selection = visibilityDraft,
                        targets = visibilityTargets,
                        onSelectionChange = {
                            visibilityDraft = it
                            deleteArmed = false
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallActionButton(
                            modifier = Modifier.weight(weight = 1f),
                            text = "取消",
                            enabled = !processing,
                            onClick = {
                                editingVisibility = false
                                visibilityDraft = tab.manifest.visibility.toSelection()
                                localMessage = null
                            }
                        )
                        SmallActionButton(
                            modifier = Modifier.weight(weight = 1f),
                            text = if (processing) "保存中" else "保存权限",
                            enabled = !processing,
                            onClick = {
                                val visibilityError = visibilityDraft.validate()
                                if (visibilityError != null) {
                                    localMessage = visibilityError
                                    return@SmallActionButton
                                }
                                processing = true
                                scope.launch {
                                    val request = UpdateCustomWebTabRequest(
                                        displayName = tab.displayName,
                                        description = tab.manifest.description.orEmpty()
                                            .ifBlank { "企业自定义 Web 业务应用" },
                                        icon = tab.manifest.icon ?: "web",
                                        entryUri = tab.manifest.entryUri.orEmpty(),
                                        sortOrder = tab.manifest.sortOrder.takeIf { it != Int.MAX_VALUE },
                                        visibility = visibilityDraft.toRequest()
                                    )
                                    when (val result = repository.updateCustomWebTab(tabId = tab.id, request = request)) {
                                        is OpenApiResult.Success -> {
                                            onChanged("已更新 ${result.data.tab?.displayName ?: tab.displayName} 的可见范围")
                                        }

                                        is OpenApiResult.Failed -> {
                                            localMessage = result.toTabActionMessage(prefix = "权限更新失败")
                                        }
                                    }
                                    processing = false
                                }
                            }
                        )
                    }
                } else {
                    ManagementActionRow(
                        icon = Icons.Rounded.Group,
                        title = "权限编辑",
                        subtitle = if (canEditConfig) {
                            "调整全公司、部门或员工可见范围"
                        } else {
                            "系统业务暂不支持客户端编辑权限"
                        },
                        enabled = canEditConfig && !processing,
                        onClick = {
                            editingVisibility = true
                            deleteArmed = false
                            localMessage = null
                        }
                    )
                    ManagementActionRow(
                        icon = if (isEnabledOnWorkbench) Icons.Rounded.Delete else Icons.Rounded.Add,
                        title = if (isEnabledOnWorkbench) "停用" else "启用",
                        subtitle = if (!canToggleWorkbench) {
                            "客户端内置或协议注册入口暂不支持服务端启停"
                        } else if (isEnabledOnWorkbench) {
                            "从我的工作台隐藏这个业务入口"
                        } else {
                            "启用到我的工作台"
                        },
                        destructive = isEnabledOnWorkbench,
                        enabled = canToggleWorkbench && !processing,
                        onClick = {
                            processing = true
                            deleteArmed = false
                            scope.launch {
                                val result = if (isEnabledOnWorkbench) {
                                    repository.disableTab(tabId = tab.id)
                                } else {
                                    repository.enableTab(tabId = tab.id)
                                }
                                when (result) {
                                    is OpenApiResult.Success -> {
                                        onChanged(
                                            if (isEnabledOnWorkbench) {
                                                "已停用 ${tab.displayName}"
                                            } else {
                                                "已启用 ${tab.displayName}"
                                            }
                                        )
                                    }

                                    is OpenApiResult.Failed -> {
                                        if (result.code == "FORBIDDEN" && isEnabledOnWorkbench) {
                                            localMessage = result.toTabActionMessage(
                                                prefix = if (isEnabledOnWorkbench) "停用失败" else "启用失败"
                                            )
                                        } else {
                                            onChanged(
                                                if (isEnabledOnWorkbench) {
                                                    "已在本机停用 ${tab.displayName}，服务端同步稍后重试"
                                                } else if (result.code == "FORBIDDEN") {
                                                    "已启用 ${tab.displayName}，打开时会进行权限校验"
                                                } else {
                                                    "已在本机启用 ${tab.displayName}，服务端同步稍后重试"
                                                }
                                            )
                                        }
                                    }
                                }
                                processing = false
                            }
                        }
                    )
                    ManagementActionRow(
                        icon = Icons.Rounded.Delete,
                        title = if (deleteArmed) "确认删除" else "删除",
                        subtitle = if (canEditConfig) {
                            "删除该自定义业务配置，并从目标成员工作台移除"
                        } else {
                            "系统业务不能在客户端删除"
                        },
                        destructive = true,
                        enabled = canEditConfig && !processing,
                        onClick = {
                            if (!deleteArmed) {
                                deleteArmed = true
                                localMessage = "删除后目标成员工作台会移除该入口，再次点击确认删除。"
                                return@ManagementActionRow
                            }
                            processing = true
                            scope.launch {
                                when (val result = repository.deleteCustomTab(tabId = tab.id)) {
                                    is OpenApiResult.Success -> onChanged("已删除 ${tab.displayName}")
                                    is OpenApiResult.Failed -> {
                                        localMessage = result.toTabActionMessage(prefix = "删除失败")
                                    }
                                }
                                processing = false
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !processing,
                onClick = onDismiss
            ) {
                Text(text = "关闭")
            }
        }
    )
}

@Composable
private fun ManagementActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val color = when {
        !enabled -> Color(color = 0xFF94A3B8)
        destructive -> Color(color = 0xFFB91C1C)
        else -> Color(color = 0xFF2563EB)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 22.dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 3.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                } else {
                    Color(color = 0xFF94A3B8)
                }
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
    }
}

@Composable
private fun CustomWebTabForm(
    repository: OpenTabRepository,
    visibilityTargets: VisibilityTargets = DefaultVisibilityTargets,
    onCancel: () -> Unit,
    onCreated: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedCategoryKey by remember { mutableStateOf(WorkbenchCategoryKeys.Company) }
    var displayName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var entryUri by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("web") }
    var customIconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showExampleDialog by remember { mutableStateOf(false) }
    var visibilityDraft by remember {
        mutableStateOf(
            VisibilitySelection(
                scope = VisibilityScope.Company
            )
        )
    }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customIconUri = uri
            icon = "custom"
        }
    }

    if (showExampleDialog) {
        TikTokExampleDialog(
            onDismiss = { showExampleDialog = false },
            onUseExample = {
                selectedCategoryKey = WorkbenchCategoryKeys.Leisure
                displayName = "TikTok 短视频"
                description = "通过 Web 页面容器接入短视频业务，支持滑动浏览和基础播放。"
                entryUri = "https://www.tiktok.com/zh-Hans"
                icon = "video"
                customIconUri = null
                showExampleDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(title = "基础信息")
            SmallIconTextButton(
                icon = Icons.Rounded.SmartDisplay,
                text = "查看示例",
                onClick = { showExampleDialog = true }
            )
        }
        ManageTextField(value = displayName, label = "应用名称", onValueChange = { displayName = it })
        ManageTextField(value = description, label = "应用描述", onValueChange = { description = it })
        ManageTextField(value = entryUri, label = "入口 URL（http/https）", onValueChange = { entryUri = it.trim() })
        CategorySelector(
            selectedCategoryKey = selectedCategoryKey,
            onSelectCategory = { categoryKey -> selectedCategoryKey = categoryKey }
        )
        InlineIconSelectorSummary(
            icon = icon,
            customIconUri = customIconUri,
            onSelectIcon = { selectedIcon ->
                icon = selectedIcon
                customIconUri = null
            },
            onUploadClick = {
                iconPickerLauncher.launch(
                    PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
        VisibilitySelector(
            selection = visibilityDraft,
            targets = visibilityTargets,
            onSelectionChange = { visibilityDraft = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallActionButton(
                modifier = Modifier.weight(weight = 1f),
                text = "取消",
                onClick = onCancel
            )
            SmallActionButton(
                modifier = Modifier.weight(weight = 1f),
                text = "发布",
                enabled = displayName.isNotBlank() && entryUri.isWebUrl(),
                onClick = {
                    val visibilityError = visibilityDraft.validate()
                    if (visibilityError != null) {
                        onMessage(visibilityError)
                        return@SmallActionButton
                    }
                    val category = WorkbenchCategoryOptions.first { it.key == selectedCategoryKey }
                    val id = "custom-${category.key}-${displayName.toSlug()}-${System.currentTimeMillis().toString().takeLast(5)}"
                    scope.launch {
                        val request = CreateCustomWebTabRequest(
                            id = id,
                            displayName = displayName.trim(),
                            description = description.trim().ifBlank { "企业自定义 Web 业务应用" },
                            icon = icon,
                            route = "/$id",
                            entryUri = entryUri.trim(),
                            sortOrder = category.defaultSortOrder,
                            extraConfig = mapOf(
                                "containerType" to "web",
                                "category" to category.key,
                                "allowedHosts" to entryUri.allowedHostForConfig(),
                                "fallbackAsset" to "short_video_fallback.html"
                            ),
                            visibility = visibilityDraft.toRequest()
                        )
                        when (val result = repository.createCustomWebTab(request = request)) {
                            is OpenApiResult.Success -> {
                                onCreated()
                                displayName = ""
                                description = ""
                                entryUri = ""
                                customIconUri = null
                                icon = "web"
                                visibilityDraft = VisibilitySelection(scope = VisibilityScope.Company)
                            }

                            is OpenApiResult.Failed -> {
                                onMessage(
                                    when (result.code) {
                                        "FORBIDDEN" -> "当前账号无权发布到该范围"
                                        "INVALID_TAB_VISIBILITY" -> "可见范围不合法，请重新选择"
                                        "TARGET_NOT_FOUND" -> "指定的部门或员工不存在"
                                        else -> "创建失败：${result.message}"
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun TikTokExampleDialog(
    onDismiss: () -> Unit,
    onUseExample: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Web 业务示例") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                Text(text = "TikTok 短视频", fontWeight = FontWeight.Bold)
                Text(text = "名称：TikTok 短视频")
                Text(text = "URL：https://www.tiktok.com/zh-Hans")
                Text(text = "分类：休闲服务")
                Text(text = "图标：视频")
            }
        },
        confirmButton = {
            TextButton(onClick = onUseExample) {
                Text(text = "填入示例")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun InlineIconSelectorSummary(
    icon: String,
    customIconUri: android.net.Uri?,
    onSelectIcon: (String) -> Unit,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = "应用图标",
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        ContainerIconOptions.chunked(size = 3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowOptions.forEach { option ->
                    ContainerIconOptionButton(
                        modifier = Modifier.weight(weight = 1f),
                        option = option,
                        selected = customIconUri == null && icon == option.value,
                        onClick = { onSelectIcon(option.value) }
                    )
                }
                repeat(3 - rowOptions.size) {
                    Box(modifier = Modifier.weight(weight = 1f))
                }
            }
        }
        AlbumIconOptionButton(
            selected = customIconUri != null,
            onClick = onUploadClick
        )
    }
}

@Composable
private fun AlbumIconOptionButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (selected) {
                    Color(color = 0xFFE0F2FE)
                } else {
                    AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 20.dp),
            imageVector = Icons.Rounded.ColorLens,
            contentDescription = null,
            tint = if (selected) Color(color = 0xFF0284C7) else AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = if (selected) "更换相册图片" else "相册选择",
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color(color = 0xFF0284C7) else AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
    }
}

@Composable
private fun VisibilitySelector(
    selection: VisibilitySelection,
    targets: VisibilityTargets,
    onSelectionChange: (VisibilitySelection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = "可见范围",
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        VisibilityScope.entries.chunked(size = 2).forEach { rowScopes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowScopes.forEach { scope ->
                    SelectionButton(
                        modifier = Modifier.weight(weight = 1f),
                        title = scope.title,
                        selected = selection.scope == scope,
                        onClick = {
                            onSelectionChange(
                                selection.copy(
                                    scope = scope,
                                    teamIds = if (scope == VisibilityScope.Team) selection.teamIds else emptySet(),
                                    userIds = if (scope == VisibilityScope.User) selection.userIds else emptySet()
                                )
                            )
                        }
                    )
                }
                repeat(2 - rowScopes.size) {
                    Box(modifier = Modifier.weight(weight = 1f))
                }
            }
        }
        when (selection.scope) {
            VisibilityScope.Team -> {
                if (targets.teamTargets.isEmpty()) {
                    InfoBanner(text = "暂无可选部门，请先在团队管理中维护部门。")
                }
                targets.teamTargets.forEach { target ->
                    TargetToggleRow(
                        title = target.name,
                        subtitle = target.description,
                        selected = target.id in selection.teamIds,
                        onClick = {
                            onSelectionChange(selection.copy(teamIds = selection.teamIds.toggle(target.id)))
                        }
                    )
                }
            }

            VisibilityScope.User -> {
                if (targets.userTargets.isEmpty()) {
                    InfoBanner(text = "暂无可选员工，请先在团队管理中维护成员。")
                }
                targets.userTargets.forEach { target ->
                    TargetToggleRow(
                        title = target.name,
                        subtitle = target.description,
                        selected = target.id in selection.userIds,
                        onClick = {
                            onSelectionChange(selection.copy(userIds = selection.userIds.toggle(target.id)))
                        }
                    )
                }
            }

            VisibilityScope.Self,
            VisibilityScope.Company -> Unit
        }
    }
}

@Composable
private fun SelectionButton(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (selected) {
                    Color(color = 0xFFEAF4FF)
                } else {
                    AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = title,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        if (selected) {
            Icon(
                modifier = Modifier.size(size = 16.dp),
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color(color = 0xFF2563EB)
            )
        }
    }
}

@Composable
private fun TargetToggleRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 20.dp)
                .clip(shape = RoundedCornerShape(size = 6.dp))
                .background(color = if (selected) Color(color = 0xFF2563EB) else Color(color = 0xFFE5E7EB)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    modifier = Modifier.size(size = 14.dp),
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategoryKey: String,
    onSelectCategory: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = "工作台分类",
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        WorkbenchCategoryOptions.chunked(size = 2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowOptions.forEach { option ->
                    CategoryOptionButton(
                        modifier = Modifier.weight(weight = 1f),
                        option = option,
                        selected = selectedCategoryKey == option.key,
                        onClick = { onSelectCategory(option.key) }
                    )
                }
                repeat(2 - rowOptions.size) {
                    Box(modifier = Modifier.weight(weight = 1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryOptionButton(
    modifier: Modifier,
    option: WorkbenchCategoryOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (selected) {
                    option.color.copy(alpha = 0.15f)
                } else {
                    AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 18.dp),
            imageVector = option.icon,
            contentDescription = null,
            tint = option.color
        )
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = option.title,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
    }
}

@Composable
private fun ContainerIconOptionButton(
    modifier: Modifier,
    option: ContainerIconOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (selected) {
                    option.color.copy(alpha = 0.16f)
                } else {
                    AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(space = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(size = 22.dp),
            imageVector = OpenTabRegistry.iconOf(icon = option.value),
            contentDescription = null,
            tint = if (selected) {
                option.color
            } else {
                option.color.copy(alpha = 0.82f)
            }
        )
        Text(
            text = option.label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = if (selected) {
                option.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
    }
}

@Composable
private fun ManageTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = {
            Text(text = label)
        },
        colors = OutlinedTextFieldDefaults.colors(
            cursorColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
            focusedBorderColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.7f),
            unfocusedBorderColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.35f)
        )
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 17.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
    )
}

@Composable
private fun InfoBanner(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        text = text,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
    )
}

@Composable
private fun SmallActionButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (enabled) {
                    AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                } else {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            color = if (enabled) {
                AppTheme.colorScheme.c_FFFFFFFF_FFFFFFFF.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
    }
}

@Composable
private fun SmallIconTextButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (destructive) {
                    Color(color = 0xFFFEF2F2)
                } else {
                    AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (destructive) {
            Color(color = 0xFFB91C1C)
        } else {
            AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        }
        Icon(
            modifier = Modifier.size(size = 16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = color
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

private fun OpenTabItem.toWorkbenchDescription(): String {
    return when (manifest.route) {
        "/approval" -> "查看待处理审批与审批记录"
        "/calendar" -> "查看团队日程安排"
        "/finance" -> "查看财务相关信息"
        "/ai-oncall" -> "咨询接入、接口和配置问题"
        else -> manifest.description.orEmpty().ifBlank { "点击打开业务应用" }
    }
}

private fun OpenTabSource.toDisplayText(): String {
    return when (this) {
        OpenTabSource.Remote -> "服务端下发"
        OpenTabSource.ClientBuiltIn -> "客户端内置"
        OpenTabSource.ProtocolRegistered -> "协议注册"
        OpenTabSource.LocalMock -> "本地兜底"
    }
}

private fun String.isWebUrl(): Boolean {
    return startsWith("http://") || startsWith("https://")
}

private fun String.allowedHostForConfig(): String {
    return runCatching {
        android.net.Uri.parse(this).host.orEmpty().removePrefix("www.")
    }.getOrDefault("").ifBlank { "unknown" }
}

private fun String.toSlug(): String {
    val normalized = lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return normalized.ifBlank { "web" }
}

private fun OpenTabItem.isEditableCustomWebTab(): Boolean {
    return manifest.entryType == EntryType.Web &&
        source == OpenTabSource.Remote &&
        id.startsWith("custom-")
}

private fun OpenTabItem.isAiOnCallTab(): Boolean {
    return id == "ai-oncall"
}

private fun OpenTabItem.isEnabledOnWorkbench(): Boolean {
    return openState != OpenTabState.Disabled && manifest.enabled
}

private fun OpenTabItem.canUseServerWorkbenchMutation(): Boolean {
    return source == OpenTabSource.Remote
}

private enum class TabManagePageMode {
    List,
    ContainerType,
    CreateCustomWeb
}

private enum class VisibilityScope(
    val title: String,
    val requestScope: String
) {
    Self(title = "仅自己", requestScope = "self"),
    Company(title = "全公司", requestScope = "company"),
    Team(title = "指定部门", requestScope = "custom"),
    User(title = "指定员工", requestScope = "custom")
}

private data class VisibilitySelection(
    val scope: VisibilityScope,
    val teamIds: Set<String> = emptySet(),
    val userIds: Set<String> = emptySet(),
    val defaultEnabled: Boolean = true
)

private data class VisibilityTarget(
    val id: String,
    val name: String,
    val description: String? = null
)

private data class VisibilityTargets(
    val teamTargets: List<VisibilityTarget>,
    val userTargets: List<VisibilityTarget>
)

private val DefaultVisibilityTargets = VisibilityTargets(
    teamTargets = listOf(
        VisibilityTarget(
            id = "team-product",
            name = "产品研发部",
            description = "负责产品设计、客户端和服务端联调"
        ),
        VisibilityTarget(
            id = "team-operation",
            name = "运营支持部",
            description = "负责运营支持、客户协同和活动执行"
        )
    ),
    userTargets = listOf(
        VisibilityTarget(
            id = "user-product-manager",
            name = "产品主管",
            description = "product-manager · 产品研发部"
        ),
        VisibilityTarget(
            id = "user-product-employee",
            name = "产品员工",
            description = "product-employee · 产品研发部"
        ),
        VisibilityTarget(
            id = "user-operation-manager",
            name = "运营主管",
            description = "operation-manager · 运营支持部"
        ),
        VisibilityTarget(
            id = "user-operation-employee",
            name = "运营员工",
            description = "operation-employee · 运营支持部"
        ),
        VisibilityTarget(
            id = "user-guest",
            name = "OpenTab 访客",
            description = "opentab-guest · 公司访客"
        )
    )
)

private fun VisibilitySelection.validate(): String? {
    return when {
        scope == VisibilityScope.Team && teamIds.isEmpty() -> "请选择至少一个部门"
        scope == VisibilityScope.User && userIds.isEmpty() -> "请选择至少一名员工"
        else -> null
    }
}

private fun VisibilitySelection.toRequest(): TabVisibilityRequest {
    return TabVisibilityRequest(
        scope = scope.requestScope,
        teamIds = if (scope == VisibilityScope.Team) teamIds.toList() else emptyList(),
        userIds = if (scope == VisibilityScope.User) userIds.toList() else emptyList(),
        defaultEnabled = defaultEnabled
    )
}

private fun TabVisibilityRequest?.toSelection(): VisibilitySelection {
    this ?: return VisibilitySelection(scope = VisibilityScope.Company)
    return when {
        scope == "company" -> VisibilitySelection(
            scope = VisibilityScope.Company,
            defaultEnabled = defaultEnabled
        )

        scope == "custom" && teamIds.isNotEmpty() -> VisibilitySelection(
            scope = VisibilityScope.Team,
            teamIds = teamIds.toSet(),
            defaultEnabled = defaultEnabled
        )

        scope == "custom" && userIds.isNotEmpty() -> VisibilitySelection(
            scope = VisibilityScope.User,
            userIds = userIds.toSet(),
            defaultEnabled = defaultEnabled
        )

        else -> VisibilitySelection(
            scope = VisibilityScope.Self,
            defaultEnabled = defaultEnabled
        )
    }
}

private fun TabVisibilityRequest?.toVisibilityText(targets: VisibilityTargets): String {
    val selection = toSelection()
    return when (selection.scope) {
        VisibilityScope.Self -> "仅创建者可见"
        VisibilityScope.Company -> "全公司可见"
        VisibilityScope.Team -> {
            val names = selection.teamIds.map { id ->
                targets.teamTargets.firstOrNull { target -> target.id == id }?.name ?: id
            }
            "指定部门：${names.joinToString().ifBlank { "未选择" }}"
        }

        VisibilityScope.User -> {
            val names = selection.userIds.map { id ->
                targets.userTargets.firstOrNull { target -> target.id == id }?.name ?: id
            }
            "指定员工：${names.joinToString().ifBlank { "未选择" }}"
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}

private fun OpenApiResult.Failed.toTabActionMessage(prefix: String): String {
    return when (code) {
        "FORBIDDEN" -> "$prefix：当前账号权限不足"
        "INVALID_TAB_VISIBILITY" -> "$prefix：可见范围不合法，请重新选择"
        "TARGET_NOT_FOUND" -> "$prefix：指定的部门或员工不存在"
        "TAB_NOT_FOUND" -> "$prefix：业务不存在或已被删除"
        else -> "$prefix：$message"
    }
}

private enum class BusinessContainerType(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val enabled: Boolean
) {
    Web(
        title = "Web 页面容器",
        description = "企业提供业务 URL，客户端用 WebView 承载，适合快速接入。",
        icon = Icons.Rounded.Language,
        color = Color(color = 0xFF2563EB),
        enabled = true
    ),
    Native(
        title = "原生组件容器",
        description = "适合高性能业务，后续开放 Fragment 接口、生命周期和隔离规范。",
        icon = Icons.Rounded.Extension,
        color = Color(color = 0xFF7C3AED),
        enabled = false
    )
}

private data class ContainerIconOption(
    val label: String,
    val value: String,
    val color: Color
)

private val ContainerIconOptions = listOf(
    ContainerIconOption(label = "样式 1", value = "shape-star", color = Color(color = 0xFF2563EB)),
    ContainerIconOption(label = "样式 2", value = "shape-bolt", color = Color(color = 0xFFF59E0B)),
    ContainerIconOption(label = "样式 3", value = "shape-gem", color = Color(color = 0xFF8B5CF6)),
    ContainerIconOption(label = "样式 4", value = "shape-extension", color = Color(color = 0xFF10B981)),
    ContainerIconOption(label = "样式 5", value = "shape-widgets", color = Color(color = 0xFFEF4444)),
    ContainerIconOption(label = "样式 6", value = "shape-bubble", color = Color(color = 0xFF0891B2)),
    ContainerIconOption(label = "样式 7", value = "shape-spark", color = Color(color = 0xFFDB2777)),
    ContainerIconOption(label = "样式 8", value = "shape-category", color = Color(color = 0xFF7C3AED)),
    ContainerIconOption(label = "样式 9", value = "shape-globe", color = Color(color = 0xFF16A34A)),
    ContainerIconOption(label = "样式 10", value = "shape-heart", color = Color(color = 0xFFE11D48))
)

private data class WorkbenchCategory(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val routeKeys: List<String>
)

private fun WorkbenchCategory.matches(tab: OpenTabItem): Boolean {
    return routeKeys.any { routeKey -> tab.id == routeKey || tab.manifest.route == routeKey } ||
        tab.manifest.extraConfig["category"] == key ||
        tab.id.startsWith("custom-$key-") ||
        tab.manifest.route.startsWith("/custom-$key-")
}

private object WorkbenchCategoryKeys {
    const val Company = "company"
    const val Collaboration = "collaboration"
    const val Leisure = "leisure"
    const val Admin = "admin"
    const val Business = "business"
}

private val WorkbenchCategories = listOf(
    WorkbenchCategory(
        key = WorkbenchCategoryKeys.Company,
        title = "企业信息",
        icon = Icons.Rounded.Groups,
        color = Color(color = 0xFF2563EB),
        routeKeys = listOf("company", "company-intro", "announcements", "/company", "/company-intro", "/announcements")
    ),
    WorkbenchCategory(
        key = WorkbenchCategoryKeys.Collaboration,
        title = "团队协作",
        icon = Icons.Rounded.WbSunny,
        color = Color(color = 0xFF16A34A),
        routeKeys = listOf("approval", "calendar", "/approval", "/calendar")
    ),
    WorkbenchCategory(
        key = WorkbenchCategoryKeys.Leisure,
        title = "休闲服务",
        icon = Icons.Rounded.EmojiEmotions,
        color = Color(color = 0xFFF59E0B),
        routeKeys = listOf("fun", "tiktok-short-video", "/fun", "/tiktok")
    ),
    WorkbenchCategory(
        key = WorkbenchCategoryKeys.Admin,
        title = "管理能力",
        icon = Icons.Rounded.AdminPanelSettings,
        color = Color(color = 0xFF7C3AED),
        routeKeys = listOf("permission-admin", "protocol-guide", "/permission-admin", "/protocol-guide")
    ),
    WorkbenchCategory(
        key = WorkbenchCategoryKeys.Business,
        title = "经营看板",
        icon = Icons.Rounded.ColorLens,
        color = Color(color = 0xFF0891B2),
        routeKeys = listOf("finance", "/finance")
    )
)

private val WorkbenchCustomCategory = WorkbenchCategory(
    key = "custom",
    title = "其他应用",
    icon = Icons.Rounded.Campaign,
    color = Color(color = 0xFF64748B),
    routeKeys = emptyList()
)

private data class WorkbenchCategoryOption(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val defaultSortOrder: Int
)

private val WorkbenchCategoryOptions = WorkbenchCategories.mapIndexed { index, category ->
    WorkbenchCategoryOption(
        key = category.key,
        title = category.title,
        icon = category.icon,
        color = category.color,
        defaultSortOrder = 200 + index * 20
    )
}
