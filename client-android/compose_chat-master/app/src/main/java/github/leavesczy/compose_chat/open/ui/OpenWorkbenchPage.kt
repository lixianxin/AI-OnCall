package github.leavesczy.compose_chat.open.ui

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.open.model.CreateCustomWebTabRequest
import github.leavesczy.compose_chat.open.repository.OpenBusinessRepository
import github.leavesczy.compose_chat.open.model.UpdateCustomWebTabRequest
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OpenTabRepository
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabRegistry
import github.leavesczy.compose_chat.open.tab.OpenTabSource
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.ui.theme.AppTheme
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
        WorkbenchTeamLine()
        WorkbenchSummary(openTabs = businessTabs)
        if (manageMode) {
            TabManagePanel(
                openTabs = businessTabs,
                repository = repository,
                businessRepository = remember { OpenBusinessRepository() },
                onRefreshTabs = onRefreshTabs
            )
        } else {
            SectionTitle(title = "业务应用")
            WorkbenchGrid(
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
                contentDescription = if (manageMode) "完成管理" else "管理 Tab",
                onClick = onClickManage
            )
        }
    }
}

@Composable
private fun WorkbenchTeamLine() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 20.dp),
            imageVector = Icons.Rounded.Group,
            contentDescription = null,
            tint = Color(color = 0xFF2563EB)
        )
        Text(
            text = "开放容器团队",
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun WorkbenchSummary(openTabs: List<OpenTabItem>) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        SummaryCell(
            modifier = Modifier.weight(weight = 1f),
            label = "应用",
            value = openTabs.size.toString()
        )
        SummaryCell(
            modifier = Modifier.weight(weight = 1f),
            label = "可用",
            value = openTabs.count { it.openState == OpenTabState.Openable }.toString()
        )
        SummaryCell(
            modifier = Modifier.weight(weight = 1f),
            label = "需处理",
            value = openTabs.count { it.openState != OpenTabState.Openable }.toString()
        )
    }
}

@Composable
private fun SummaryCell(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = Color(color = 0xFF6B7280)
        )
    }
}

@Composable
private fun WorkbenchGrid(
    businessTabs: List<OpenTabItem>,
    onClickOpenTab: (OpenTabItem) -> Unit
) {
    if (businessTabs.isEmpty()) {
        InfoBanner(text = "当前还没有可展示的业务应用，请刷新或进入管理模式添加入口。")
        return
    }
    businessTabs.chunked(size = 2).forEach { rowTabs ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            rowTabs.forEach { tab ->
                WorkbenchItem(
                    modifier = Modifier.weight(weight = 1f),
                    tab = tab,
                    onClick = { onClickOpenTab(tab) }
                )
            }
            if (rowTabs.size == 1) {
                Column(modifier = Modifier.weight(weight = 1f)) {
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
            .clip(shape = RoundedCornerShape(size = 16.dp))
            .background(color = Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 14.dp))
                .background(color = Color(color = 0xFFEFF6FF))
                .padding(all = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 28.dp),
                imageVector = tab.icon,
                contentDescription = null,
                tint = Color(color = 0xFF2563EB)
            )
        }
        Text(
            text = tab.displayName,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
        StateBadge(
            text = tab.openState.toWorkbenchStateText(),
            openState = tab.openState
        )
        Text(
            text = tab.toWorkbenchDescription(),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color(color = 0xFF6B7280)
        )
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
    businessRepository: OpenBusinessRepository,
    onRefreshTabs: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<List<OpenTabItem>>(emptyList()) }
    var loadingCatalog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    fun refreshCatalog() {
        loadingCatalog = true
        scope.launch {
            when (val result = repository.getCatalogTabs()) {
                is OpenApiResult.Success -> {
                    catalog = repository.buildItems(result.data)
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
        SectionTitle(title = "已启用")
        openTabs.forEach { tab ->
            ManageTabRow(
                tab = tab,
                actionText = "停用",
                actionEnabled = tab.id != "ai-oncall",
                onAction = {
                    scope.launch {
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
        SectionTitle(title = "可添加的内置 Tab")
        val enabledIds = openTabs.map { it.id }.toSet()
        if (loadingCatalog) {
            InfoBanner(text = "正在加载系统 Tab 目录…")
        }
        catalog.filterNot { it.id in enabledIds || it.id == "ai-oncall" }.forEach { tab ->
            ManageTabRow(
                tab = tab,
                actionText = if (tab.openState == OpenTabState.PermissionDenied) "无权限" else "启用",
                actionEnabled = tab.openState != OpenTabState.PermissionDenied,
                onAction = {
                    scope.launch {
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
                    }
                }
            )
        }
        SectionTitle(title = "新增自定义网页 Tab")
        CustomWebTabForm(
            repository = repository,
            onCreated = {
                actionMessage = "自定义 Tab 已创建"
                onRefreshTabs()
                refreshCatalog()
            },
            onMessage = { message ->
                actionMessage = message
            }
        )
        SectionTitle(title = "接口调试")
        DebugProtocolPanel(repository = businessRepository)
    }
}

@Composable
private fun DebugProtocolPanel(repository: OpenBusinessRepository) {
    val scope = rememberCoroutineScope()
    var permissionsText by remember { mutableStateOf("点击加载服务端权限列表") }
    var sampleTabsText by remember { mutableStateOf("点击加载示例 TabManifest") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        Text(
            text = "用于和服务端协议对齐：权限码来自 /debug/permissions，示例 Tab 来自 /debug/sample-tabs。",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            SmallActionButton(
                text = "权限列表",
                onClick = {
                    scope.launch {
                        permissionsText = when (val result = repository.debugPermissions()) {
                            is OpenApiResult.Success -> result.data.joinToString(separator = "\n") { item ->
                                "${item.code}：${item.description}"
                            }.ifBlank { "服务端未返回权限数据" }

                            is OpenApiResult.Failed -> "权限列表加载失败：${result.message}"
                        }
                    }
                }
            )
            SmallActionButton(
                text = "示例 Tab",
                onClick = {
                    scope.launch {
                        sampleTabsText = when (val result = repository.debugSampleTabs()) {
                            is OpenApiResult.Success -> result.data.joinToString(separator = "\n") { tab ->
                                "${tab.id} · ${tab.displayName} · ${tab.entryType} · ${tab.route}"
                            }.ifBlank { "服务端未返回示例 Tab" }

                            is OpenApiResult.Failed -> "示例 Tab 加载失败：${result.message}"
                        }
                    }
                }
            )
        }
        DebugTextBlock(title = "权限码", body = permissionsText)
        DebugTextBlock(title = "示例 TabManifest", body = sampleTabsText)
    }
}

@Composable
private fun DebugTextBlock(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(space = 5.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            text = body,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
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
        ManageTextField(value = displayName, label = "Tab 名称", onValueChange = { displayName = it })
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
                            description = description.trim().ifBlank { "用户自定义网页 Tab" },
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
    onCreated: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var entryUri by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("docs") }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        Text(
            text = "第一版自定义 Tab 只支持网页入口。保存后会调用服务端 `POST /tabs`，并默认启用到当前账号。",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        ManageTextField(value = displayName, label = "Tab 名称", onValueChange = { displayName = it })
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
        PreviewCustomTabCard(
            displayName = displayName.ifBlank { "未命名网页" },
            description = description.ifBlank { "用户自定义网页 Tab" },
            icon = icon,
            entryUri = entryUri.ifBlank { "https://example.com" }
        )
        SmallActionButton(
            text = "保存自定义网页",
            enabled = displayName.isNotBlank() && entryUri.isWebUrl(),
            onClick = {
                val id = "custom-${displayName.toSlug()}-${System.currentTimeMillis().toString().takeLast(5)}"
                scope.launch {
                    val request = CreateCustomWebTabRequest(
                        id = id,
                        displayName = displayName.trim(),
                        description = description.trim().ifBlank { "用户自定义网页 Tab" },
                        icon = icon,
                        route = "/$id",
                        entryUri = entryUri.trim()
                    )
                    when (val result = repository.createCustomWebTab(request = request)) {
                        is OpenApiResult.Success -> {
                            onCreated()
                            displayName = ""
                            description = ""
                            entryUri = ""
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
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
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
        OpenTabSource.LocalMock -> "本地兜底"
    }
}

private fun String.isWebUrl(): Boolean {
    return startsWith("http://") || startsWith("https://")
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
