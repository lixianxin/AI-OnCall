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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import github.leavesczy.compose_chat.open.model.UpdateCustomWebTabRequest
import github.leavesczy.compose_chat.open.network.OpenApiResult
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
        tab.id == "ai-oncall"
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
        WorkbenchHeader(
            manageMode = manageMode,
            onClickManage = { manageMode = !manageMode },
            onRefresh = onRefreshTabs
        )
        if (manageMode) {
            TabManagePanel(
                openTabs = businessTabs,
                repository = repository,
                onRefreshTabs = onRefreshTabs
            )
        } else {
            WorkbenchHeroBanner()
            WorkbenchCategoryList(
                businessTabs = businessTabs,
                onClickOpenTab = onClickOpenTab
            )
        }
    }
}

@Composable
private fun WorkbenchHeader(
    manageMode: Boolean,
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
                icon = if (manageMode) Icons.Rounded.Check else Icons.Rounded.Settings,
                contentDescription = if (manageMode) "完成管理" else "管理业务应用",
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
        if (tab.openState != OpenTabState.Openable) {
            StateBadge(
                text = tab.openState.toWorkbenchStateText(),
                openState = tab.openState
            )
        }
    }
}

@Composable
private fun StateBadge(
    text: String,
    openState: OpenTabState
) {
    val backgroundColor = when (openState) {
        OpenTabState.Openable -> Color(color = 0xFFEAF7EF)
        OpenTabState.PermissionDenied -> Color(color = 0xFFFFF7ED)
        OpenTabState.VersionIncompatible -> Color(color = 0xFFFEF2F2)
        else -> Color(color = 0xFFF3F4F6)
    }
    val textColor = when (openState) {
        OpenTabState.Openable -> Color(color = 0xFF15803D)
        OpenTabState.PermissionDenied -> Color(color = 0xFFC2410C)
        OpenTabState.VersionIncompatible -> Color(color = 0xFFB91C1C)
        else -> Color(color = 0xFF4B5563)
    }
    Text(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 999.dp))
            .background(color = backgroundColor)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        text = text,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
}

@Composable
private fun TabManagePanel(
    openTabs: List<OpenTabItem>,
    repository: OpenTabRepository,
    onRefreshTabs: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<List<OpenTabItem>>(emptyList()) }
    var loadingCatalog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var pageMode by remember { mutableStateOf(TabManagePageMode.List) }
    var processingTabId by remember { mutableStateOf<String?>(null) }
    val canCreateBusinessApp = OpenSessionManager.permissions.contains("tab.admin.manage") ||
        OpenSessionManager.permissions.contains("team.manage")

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

    androidx.compose.runtime.LaunchedEffect(Unit) {
        refreshCatalog()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        if (actionMessage != null) {
            InfoBanner(text = actionMessage.orEmpty())
        }
        if (pageMode == TabManagePageMode.CreateCustomWeb) {
            CreateContainerPage(
                repository = repository,
                onBack = { pageMode = TabManagePageMode.List },
                onCreated = {
                    actionMessage = "业务应用已添加"
                    pageMode = TabManagePageMode.List
                    onRefreshTabs()
                    refreshCatalog()
                },
                onMessage = { message ->
                    actionMessage = message
                }
            )
        } else {
            if (canCreateBusinessApp) {
                CreateBusinessAppEntry(
                    onClick = { pageMode = TabManagePageMode.CreateCustomWeb }
                )
            }
            SectionTitle(title = "已启用")
            if (openTabs.isEmpty()) {
                InfoBanner(text = "当前工作台没有已启用的业务入口。")
            }
            openTabs.forEach { tab ->
                ManageTabRow(
                    tab = tab,
                    actionText = if (processingTabId == tab.id) "处理中" else "停用",
                    actionEnabled = tab.id != "ai-oncall" && processingTabId == null,
                    onAction = {
                        scope.launch {
                            processingTabId = tab.id
                            when (val result = repository.disableTab(tabId = tab.id)) {
                                is OpenApiResult.Success -> {
                                    actionMessage = "已停用 ${tab.displayName}"
                                    onRefreshTabs()
                                    refreshCatalog()
                                }

                                is OpenApiResult.Failed -> {
                                    actionMessage = "停用失败：${result.message}"
                                }
                            }
                            processingTabId = null
                        }
                    },
                    extraActions = if (tab.isEditableCustomWebTab()) {
                        {
                            CustomTabInlineEditor(
                                tab = tab,
                                repository = repository,
                                onChanged = {
                                    actionMessage = it
                                    onRefreshTabs()
                                    refreshCatalog()
                                }
                            )
                        }
                    } else {
                        null
                    }
                )
            }
            SectionTitle(title = "可添加的业务应用")
            val enabledIds = openTabs.map { it.id }.toSet()
            if (loadingCatalog) {
                InfoBanner(text = "正在加载业务应用目录…")
            }
            catalog.filterNot { it.id in enabledIds || it.id == "ai-oncall" }.forEach { tab ->
                ManageTabRow(
                    tab = tab,
                    actionText = when {
                        processingTabId == tab.id -> "处理中"
                        tab.openState == OpenTabState.PermissionDenied -> "无权限"
                        else -> "启用"
                    },
                    actionEnabled = tab.openState != OpenTabState.PermissionDenied && processingTabId == null,
                    onAction = {
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
                                        "FORBIDDEN" -> "权限不足，无法启用 ${tab.displayName}"
                                        else -> "启用失败：${result.message}"
                                    }
                                }
                            }
                            processingTabId = null
                        }
                    }
                )
            }
        }
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
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = Color(color = 0xFF2563EB)
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                text = "添加业务应用",
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF111827)
            )
            Text(
                text = "通过 Web 页面容器快速接入企业业务",
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
            text = "添加",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF2563EB)
        )
    }
}

@Composable
private fun CreateContainerPage(
    repository: OpenTabRepository,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallActionButton(text = "返回", onClick = onBack)
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 3.dp)
            ) {
                Text(
                    text = "添加业务应用",
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                Text(
                    text = "选择容器类型和工作台分类，发布后员工可直接打开。",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
        CustomWebTabForm(
            repository = repository,
            onCancel = onBack,
            onCreated = onCreated,
            onMessage = onMessage
        )
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
private fun CustomTabInlineEditor(
    tab: OpenTabItem,
    repository: OpenTabRepository,
    onChanged: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var editing by remember(tab.id) { mutableStateOf(false) }
    var displayName by remember(tab.id) { mutableStateOf(tab.displayName) }
    var description by remember(tab.id) { mutableStateOf(tab.manifest.description.orEmpty()) }
    var entryUri by remember(tab.id) { mutableStateOf(tab.manifest.entryUri.orEmpty()) }
    var icon by remember(tab.id) { mutableStateOf(tab.manifest.icon ?: "docs") }
    var menuExpanded by remember { mutableStateOf(false) }

    if (!editing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallIconTextButton(
                icon = Icons.Rounded.Edit,
                text = "编辑",
                onClick = { editing = true }
            )
            SmallIconTextButton(
                icon = Icons.Rounded.Delete,
                text = "删除配置",
                destructive = true,
                onClick = {
                    scope.launch {
                        when (val result = repository.deleteCustomTab(tabId = tab.id)) {
                            is OpenApiResult.Success -> onChanged("已删除 ${tab.displayName}")
                            is OpenApiResult.Failed -> onChanged("删除失败：${result.message}")
                        }
                    }
                }
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        ManageTextField(value = displayName, label = "应用名称", onValueChange = { displayName = it })
        ManageTextField(value = description, label = "描述", onValueChange = { description = it })
        ManageTextField(value = entryUri, label = "网页地址（http/https）", onValueChange = { entryUri = it.trim() })
        Box {
            SmallActionButton(text = "图标：$icon", onClick = { menuExpanded = true })
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                listOf("docs", "web", "approval", "calendar", "finance").forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item) },
                        onClick = {
                            icon = item
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            SmallActionButton(
                text = "保存修改",
                enabled = displayName.isNotBlank() && entryUri.isWebUrl(),
                onClick = {
                    scope.launch {
                        val request = UpdateCustomWebTabRequest(
                            displayName = displayName.trim(),
                            description = description.trim().ifBlank { "企业自定义网页应用" },
                            icon = icon,
                            entryUri = entryUri.trim(),
                            sortOrder = tab.manifest.sortOrder.takeIf { it != Int.MAX_VALUE }
                        )
                        when (val result = repository.updateCustomWebTab(tabId = tab.id, request = request)) {
                            is OpenApiResult.Success -> {
                                editing = false
                                onChanged("已更新 ${result.data.tab?.displayName ?: displayName}")
                            }

                            is OpenApiResult.Failed -> onChanged("更新失败：${result.message}")
                        }
                    }
                }
            )
            SmallActionButton(text = "取消", onClick = { editing = false })
        }
    }
}

@Composable
private fun CustomWebTabForm(
    repository: OpenTabRepository,
    onCancel: () -> Unit,
    onCreated: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var containerType by remember { mutableStateOf(BusinessContainerType.Web) }
    var selectedCategoryKey by remember { mutableStateOf(WorkbenchCategoryKeys.Leisure) }
    var displayName by remember { mutableStateOf("TikTok 短视频") }
    var description by remember { mutableStateOf("通过 Web 页面容器接入短视频业务，支持滑动浏览和基础播放。") }
    var entryUri by remember { mutableStateOf("https://www.tiktok.com/zh-Hans") }
    var icon by remember { mutableStateOf("video") }
    var customIconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customIconUri = uri
            icon = "custom"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        ContainerTypeSelector(
            selectedType = containerType,
            onSelect = { type -> containerType = type }
        )
        if (containerType == BusinessContainerType.Native) {
            InfoBanner(text = "原生组件容器适合高性能或深度集成业务，后续会开放标准 Fragment 接口、生命周期规范和组件隔离能力。当前版本先开放 Web 页面容器。")
        }
        CategorySelector(
            selectedCategoryKey = selectedCategoryKey,
            onSelectCategory = { categoryKey -> selectedCategoryKey = categoryKey }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallIconTextButton(
                icon = Icons.Rounded.SmartDisplay,
                text = "填入 TikTok 示例",
                onClick = {
                    containerType = BusinessContainerType.Web
                    selectedCategoryKey = WorkbenchCategoryKeys.Leisure
                    displayName = "TikTok 短视频"
                    description = "通过 Web 页面容器接入短视频业务，支持滑动浏览和基础播放。"
                    entryUri = "https://www.tiktok.com/zh-Hans"
                    icon = "video"
                    customIconUri = null
                }
            )
        }
        ManageTextField(value = displayName, label = "应用名称", onValueChange = { displayName = it })
        ManageTextField(value = description, label = "应用描述", onValueChange = { description = it })
        ManageTextField(value = entryUri, label = "入口 URL（http/https）", onValueChange = { entryUri = it.trim() })
        ContainerIconSelector(
            selectedIcon = icon,
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
        PreviewCustomTabCard(
            displayName = displayName.ifBlank { "业务应用名称" },
            description = "${WorkbenchCategoryOptions.first { it.key == selectedCategoryKey }.title} · ${containerType.title}",
            icon = icon,
            entryUri = entryUri.ifBlank { "等待填写入口 URL" }
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
                text = "添加",
                enabled = containerType == BusinessContainerType.Web && displayName.isNotBlank() && entryUri.isWebUrl(),
                onClick = {
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
                            )
                        )
                        when (val result = repository.createCustomWebTab(request = request)) {
                            is OpenApiResult.Success -> {
                                onCreated()
                                displayName = ""
                                description = ""
                                entryUri = ""
                                customIconUri = null
                                icon = "video"
                            }

                            is OpenApiResult.Failed -> {
                                onMessage("创建失败：${result.message}")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ContainerIconSelector(
    selectedIcon: String,
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
        ContainerIconOptions.chunked(size = 5).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowOptions.forEach { option ->
                    ContainerIconOptionButton(
                        modifier = Modifier.weight(weight = 1f),
                        option = option,
                        selected = selectedIcon == option.value,
                        onClick = { onSelectIcon(option.value) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallActionButton(
                text = if (customIconUri == null) "上传图标" else "更换图标",
                onClick = onUploadClick
            )
            customIconUri?.let { uri ->
                Row(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                        .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComponentImage(
                        modifier = Modifier
                            .size(size = 32.dp)
                            .clip(shape = RoundedCornerShape(size = 8.dp)),
                        model = uri,
                        backgroundColor = Color(color = 0xFFEFF6FF)
                    )
                    Text(
                        text = "已选择自定义图标",
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
        }
    }
}

@Composable
private fun ContainerTypeSelector(
    selectedType: BusinessContainerType,
    onSelect: (BusinessContainerType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = "容器类型",
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        BusinessContainerType.entries.forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(size = 8.dp))
                    .background(
                        color = if (selectedType == type) {
                            type.color.copy(alpha = 0.14f)
                        } else {
                            AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                        }
                    )
                    .clickable(enabled = type.enabled) {
                        onSelect(type)
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(size = 22.dp),
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = if (type.enabled) type.color else AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
                Column(
                    modifier = Modifier.weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(space = 3.dp)
                ) {
                    Text(
                        text = "${type.title} · ${if (type.enabled) "已开放" else "待开放"}",
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                    )
                    Text(
                        text = type.description,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
                if (selectedType == type) {
                    Icon(
                        modifier = Modifier.size(size = 18.dp),
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = type.color
                    )
                }
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
private fun PreviewCustomTabCard(
    displayName: String,
    description: String,
    icon: String,
    entryUri: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 24.dp),
            imageVector = OpenTabRegistry.iconOf(icon = icon),
            contentDescription = null,
            tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
        Column(verticalArrangement = Arrangement.spacedBy(space = 3.dp)) {
            Text(
                text = displayName,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "$description\n$entryUri",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
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
    return manifest.entryType == github.leavesczy.compose_chat.open.model.EntryType.Web &&
        source == OpenTabSource.Remote &&
        id.startsWith("custom-")
}

private enum class TabManagePageMode {
    List,
    CreateCustomWeb
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
