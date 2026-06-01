package github.leavesczy.compose_chat.open.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import github.leavesczy.compose_chat.open.model.ApprovalItemDto
import github.leavesczy.compose_chat.open.model.CalendarEventDto
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OpenBusinessRepository
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OpenTabContentHost(
    modifier: Modifier = Modifier,
    tab: OpenTabItem?,
    onBackToWorkbench: (() -> Unit)? = null
) {
    if (tab == null) {
        OpenStatePage(
            modifier = modifier,
            title = "未选择业务页",
            message = "当前没有需要渲染的动态业务 Tab。"
        )
        return
    }
    if (tab.openState != OpenTabState.Openable) {
        OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = tab.openState.toDisplayMessage(tab = tab),
            onBackToWorkbench = onBackToWorkbench
        )
        return
    }
    when (tab.manifest.entryType) {
        EntryType.Native -> NativeTabPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        EntryType.Web -> WebTabPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        EntryType.External -> ExternalTabPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        EntryType.Hybrid,
        EntryType.Unknown -> OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = "当前客户端暂不支持打开该业务应用。",
            onBackToWorkbench = onBackToWorkbench
        )
    }
}

@Composable
private fun NativeTabPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    when (tab.manifest.route) {
        "/approval" -> ApprovalPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/calendar" -> CalendarPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/finance" -> FinancePlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/ai-oncall" -> OnCallPlaceholderPage(modifier = modifier, onBackToWorkbench = onBackToWorkbench)
        else -> OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = "该业务页暂未接入当前客户端。",
            onBackToWorkbench = onBackToWorkbench
        )
    }
}

@Composable
private fun ApprovalPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    repository: OpenBusinessRepository = remember { OpenBusinessRepository() }
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var pendingCount by remember { mutableStateOf(0) }
    var approvedToday by remember { mutableStateOf(0) }
    var selectedFilter by remember { mutableStateOf(ApprovalFilter.All) }
    var highlightedStatus by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ApprovalItemDto>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<ApprovalItemDto?>(null) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadApprovalData() {
        loading = true
    }

    fun selectFilter(filter: ApprovalFilter) {
        selectedFilter = filter
        selectedItem = null
        selectedItemId = null
        detailLoading = false
        highlightedStatus = filter.highlightStatus
        loading = true
        scope.launch {
            scrollState.animateScrollTo(value = 260)
        }
    }

    LaunchedEffect(highlightedStatus) {
        val status = highlightedStatus ?: return@LaunchedEffect
        delay(timeMillis = 900)
        if (highlightedStatus == status) {
            highlightedStatus = null
        }
    }

    LaunchedEffect(loading, selectedFilter) {
        if (!loading) {
            return@LaunchedEffect
        }
        when (val summaryResult = repository.approvalSummary()) {
            is OpenApiResult.Success -> {
                pendingCount = summaryResult.data.pendingCount
                approvedToday = summaryResult.data.approvedToday
            }

            is OpenApiResult.Failed -> {
                errorMessage = "审批摘要加载失败：${summaryResult.message}"
            }
        }
        when (val listResult = repository.approvalItems(status = selectedFilter.requestStatus)) {
            is OpenApiResult.Success -> {
                items = listResult.data
                errorMessage = null
            }

            is OpenApiResult.Failed -> {
                errorMessage = errorMessage ?: "审批列表加载失败：${listResult.message}"
            }
        }
        loading = false
    }

    OpenTabScaffold(
        modifier = modifier,
        tab = tab,
        onBackToWorkbench = onBackToWorkbench,
        scrollState = scrollState
    ) {
        if (loading) {
            SectionCard(title = "审批数据", body = "正在从服务端加载审批列表…")
        }
        if (errorMessage != null) {
            SectionCard(title = "审批数据加载失败", body = errorMessage.orEmpty())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
            MetricRow(
                modifier = Modifier.weight(weight = 1f),
                label = "待处理审批",
                value = pendingCount.toString(),
                selected = selectedFilter == ApprovalFilter.Pending,
                onClick = { selectFilter(filter = ApprovalFilter.Pending) }
            )
            MetricRow(
                modifier = Modifier.weight(weight = 1f),
                label = "今日已通过",
                value = approvedToday.toString(),
                selected = selectedFilter == ApprovalFilter.Approved,
                onClick = { selectFilter(filter = ApprovalFilter.Approved) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallIconButton(
                text = "刷新审批",
                onClick = {
                    selectedItem = null
                    selectedItemId = null
                    detailLoading = false
                    loadApprovalData()
                }
            )
            if (selectedFilter != ApprovalFilter.All) {
                SmallIconButton(
                    text = "查看全部",
                    onClick = { selectFilter(filter = ApprovalFilter.All) }
                )
            }
        }
        Text(
            text = "当前查看：${selectedFilter.label}",
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        if (!loading && items.isEmpty()) {
            SectionCard(title = selectedFilter.emptyTitle, body = selectedFilter.emptyBody)
        }
        items.forEach { item ->
            val rowSelected = selectedItemId == item.id
            ClickableInfoRow(
                title = item.title,
                body = "${item.applicant} · ${item.status.toApprovalStatusText()} · ${item.createdAt}",
                highlighted = rowSelected || item.status == highlightedStatus,
                onClick = {
                    if (selectedItemId == item.id && selectedItem != null) {
                        selectedItem = null
                        selectedItemId = null
                        detailLoading = false
                    } else {
                        selectedItemId = item.id
                        selectedItem = null
                        detailLoading = true
                    }
                }
            )
            if (rowSelected) {
                if (detailLoading) {
                    SectionCard(title = "审批详情", body = "正在加载审批详情…")
                }
                val detail = selectedItem
                if (detail != null) {
                    DetailCard(
                        title = detail.title,
                        body = buildString {
                            appendLine("申请人：${detail.applicant}")
                            appendLine("状态：${detail.status.toApprovalStatusText()}")
                            detail.amount?.let { appendLine("金额：$it") }
                            detail.reason?.let { appendLine("原因：$it") }
                            detail.comment?.let { appendLine("备注：$it") }
                            append("创建时间：${detail.createdAt}")
                        },
                        onClose = {
                            selectedItem = null
                            selectedItemId = null
                            detailLoading = false
                        }
                    )
                }
            }
        }
        LaunchedEffect(selectedItemId) {
            val itemId = selectedItemId ?: return@LaunchedEffect
            detailLoading = true
            when (val result = repository.approvalDetail(itemId = itemId)) {
                is OpenApiResult.Success -> {
                    selectedItem = result.data
                    errorMessage = null
                }

                is OpenApiResult.Failed -> {
                    errorMessage = "审批详情加载失败：${result.message}"
                }
            }
            detailLoading = false
        }
    }
}

@Composable
private fun CalendarPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    repository: OpenBusinessRepository = remember { OpenBusinessRepository() }
) {
    var queryDate by remember { mutableStateOf("2026-06-01") }
    var todayCount by remember { mutableStateOf(0) }
    var events by remember { mutableStateOf<List<CalendarEventDto>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<CalendarEventDto?>(null) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadCalendarData() {
        loading = true
    }

    LaunchedEffect(loading) {
        if (!loading) {
            return@LaunchedEffect
        }
        when (val summaryResult = repository.calendarSummary()) {
            is OpenApiResult.Success -> {
                todayCount = summaryResult.data.todayCount
            }

            is OpenApiResult.Failed -> {
                errorMessage = "日程摘要加载失败：${summaryResult.message}"
            }
        }
        when (val eventsResult = repository.calendarEvents(date = queryDate)) {
            is OpenApiResult.Success -> {
                events = eventsResult.data
                errorMessage = null
            }

            is OpenApiResult.Failed -> {
                errorMessage = errorMessage ?: "日程列表加载失败：${eventsResult.message}"
            }
        }
        loading = false
    }

    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
            MetricRow(modifier = Modifier.weight(weight = 1f), label = "今日日程", value = todayCount.toString())
            MetricRow(modifier = Modifier.weight(weight = 1f), label = "查询日期", value = queryDate.substringAfterLast("-"))
        }
        SmallIconButton(
            text = "刷新日程",
            onClick = {
                selectedEvent = null
                selectedEventId = null
                loadCalendarData()
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            SmallIconButton(
                text = "今天",
                onClick = {
                    queryDate = "2026-06-01"
                    selectedEvent = null
                    selectedEventId = null
                    loadCalendarData()
                }
            )
            SmallIconButton(
                text = "示例日程",
                onClick = {
                    queryDate = "2026-05-31"
                    selectedEvent = null
                    selectedEventId = null
                    loadCalendarData()
                }
            )
        }
        if (loading) {
            SectionCard(title = "日程数据", body = "正在从服务端加载 $queryDate 的日程…")
        }
        if (errorMessage != null) {
            SectionCard(title = "日程数据加载失败", body = errorMessage.orEmpty())
        }
        val detail = selectedEvent
        if (detailLoading) {
            SectionCard(title = "日程详情", body = "正在加载日程详情…")
        }
        if (detail != null) {
            DetailCard(
                title = detail.title,
                body = buildString {
                    detail.description?.let { appendLine(it) }
                    appendLine("时间：${detail.startTime} - ${detail.endTime}")
                    detail.location?.let { appendLine("地点：$it") }
                    if (detail.participants.isNotEmpty()) {
                        append("参与人：${detail.participants.joinToString()}")
                    }
                },
                onClose = { selectedEvent = null }
            )
        }
        if (!loading && events.isEmpty()) {
            SectionCard(title = "暂无日程", body = "$queryDate 没有团队日程。")
        }
        events.forEach { event ->
            ClickableInfoRow(
                title = event.title,
                body = "${event.startTime} - ${event.endTime}\n${event.location.orEmpty()}",
                onClick = {
                    selectedEventId = event.id
                    detailLoading = true
                }
            )
        }
        LaunchedEffect(selectedEventId, detailLoading) {
            val eventId = selectedEventId ?: return@LaunchedEffect
            if (!detailLoading) {
                return@LaunchedEffect
            }
            when (val result = repository.calendarDetail(eventId = eventId)) {
                is OpenApiResult.Success -> {
                    selectedEvent = result.data
                    errorMessage = null
                }

                is OpenApiResult.Failed -> {
                    errorMessage = "日程详情加载失败：${result.message}"
                }
            }
            detailLoading = false
        }
    }
}

@Composable
private fun FinancePlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        SectionCard(
            title = "财务看板",
            body = "财务数据看板仍在接入中，后续会展示预算、报销和付款进度。"
        )
    }
}

@Composable
private fun OnCallPlaceholderPage(
    modifier: Modifier,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenOnCallPage(modifier = modifier, onBackToOnCallHome = onBackToWorkbench)
}

@Composable
private fun WebTabPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    WebTabPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
}

@Composable
private fun ExternalTabPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        SectionCard(
            title = "外部入口暂不可用",
            body = "该入口需要跳转到外部应用，当前客户端暂未开放直接跳转。"
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebTabPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    val entryUri = tab.manifest.entryUri.orEmpty()
    var loading by remember(entryUri) { mutableStateOf(true) }
    var errorMessage by remember(entryUri) { mutableStateOf<String?>(null) }
    var webView by remember(entryUri) { mutableStateOf<WebView?>(null) }
    var canGoBack by remember(entryUri) { mutableStateOf(false) }
    var currentUrl by remember(entryUri) { mutableStateOf(entryUri) }
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 6.dp)
        ) {
            OpenTabHeader(tab = tab, onBackToWorkbench = onBackToWorkbench)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                SmallIconButton(
                    text = "网页返回",
                    enabled = canGoBack,
                    onClick = {
                        webView?.goBack()
                    }
                )
                SmallIconButton(
                    text = "重新加载",
                    onClick = {
                        loading = true
                        errorMessage = null
                        webView?.reload()
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .fillMaxWidth()
        ) {
            if (entryUri.isBlank()) {
                OpenStatePage(
                    modifier = Modifier.fillMaxSize(),
                    title = "Web Tab 配置异常",
                    message = "服务端没有为 ${tab.displayName} 下发 entryUri。",
                    onBackToWorkbench = null
                )
                return@Box
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val nextUrl = request?.url?.toString().orEmpty()
                                if (nextUrl.startsWith("http://") || nextUrl.startsWith("https://")) {
                                    return false
                                }
                                errorMessage = "已拦截外部链接：$nextUrl"
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                canGoBack = view?.canGoBack() == true
                                currentUrl = url.orEmpty().ifBlank { entryUri }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
                                    canGoBack = view?.canGoBack() == true
                                    errorMessage = error?.description?.toString() ?: "网页加载失败"
                                }
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        loadUrl(entryUri)
                    }
                },
                update = { webView ->
                    if (webView.url != entryUri) {
                        loading = true
                        errorMessage = null
                        webView.loadUrl(entryUri)
                    }
                }
            )
            if (loading) {
                SectionCard(
                    title = "正在加载网页",
                    body = "容器正在打开 ${tab.displayName}。"
                )
            }
            if (errorMessage != null) {
                SectionCard(
                    title = "网页加载失败",
                    body = errorMessage.orEmpty()
                )
            }
        }
    }
}

@Composable
private fun OpenTabScaffold(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding()
            .verticalScroll(state = scrollState)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        OpenTabHeader(tab = tab, onBackToWorkbench = onBackToWorkbench)
        val description = tab.manifest.description.orEmpty()
        if (description.isNotBlank()) {
            Text(
                modifier = Modifier,
                text = description,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        content()
    }
}

@Composable
private fun OpenTabHeader(
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackToWorkbench != null) {
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
                    .clickable(onClick = onBackToWorkbench)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(size = 18.dp),
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
                Text(
                    text = "工作台",
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
            }
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            Text(
                text = tab.displayName,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "团队业务入口",
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
    }
}

@Composable
private fun OpenStatePage(
    modifier: Modifier,
    title: String,
    message: String,
    onBackToWorkbench: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier,
            text = title,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            modifier = Modifier,
            text = message,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        if (onBackToWorkbench != null) {
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
                    .clickable(onClick = onBackToWorkbench)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(size = 18.dp),
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
                Text(
                    text = "工作台",
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (selected) {
                    AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.14f)
                } else {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier,
            text = label,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Text(
            modifier = Modifier,
            text = value,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
    }
}

@Composable
private fun ClickableInfoRow(
    title: String,
    body: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (highlighted) {
                    AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.14f)
                } else {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 5.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = body,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        Icon(
            modifier = Modifier.size(size = 20.dp),
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    body: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 9.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = title,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            IconButton(
                modifier = Modifier.size(size = 34.dp),
                onClick = onClose
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "关闭详情",
                    tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
        Text(
            text = body,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

@Composable
private fun SmallIconButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (enabled) {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                } else {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color.copy(alpha = 0.5f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 16.dp),
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = if (enabled) {
                AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = if (enabled) {
                AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier,
            text = title,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            modifier = Modifier,
            text = body,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

private fun OpenTabState.toDisplayMessage(tab: OpenTabItem): String {
    return when (this) {
        OpenTabState.Openable -> "该业务 Tab 可以打开。"
        OpenTabState.Disabled -> "该业务应用暂未启用。"
        OpenTabState.PermissionDenied -> "当前账号暂无访问权限，请联系管理员开通。"
        OpenTabState.VersionIncompatible -> "当前客户端版本过低，请升级后再打开。"
        OpenTabState.RouteUnsupported -> "该业务页暂未接入当前客户端。"
        OpenTabState.EntryUnsupported -> "当前客户端暂不支持打开该业务应用。"
        OpenTabState.InvalidConfig -> "该业务应用配置不完整，暂时无法打开。"
    }
}

private enum class ApprovalFilter(
    val label: String,
    val requestStatus: String,
    val highlightStatus: String?,
    val emptyTitle: String,
    val emptyBody: String
) {
    All(
        label = "全部审批",
        requestStatus = "all",
        highlightStatus = null,
        emptyTitle = "暂无审批",
        emptyBody = "当前账号没有审批记录。"
    ),
    Pending(
        label = "待处理审批",
        requestStatus = "pending",
        highlightStatus = "pending",
        emptyTitle = "暂无待处理审批",
        emptyBody = "当前没有需要处理的审批。"
    ),
    Approved(
        label = "已通过审批",
        requestStatus = "approved",
        highlightStatus = "approved",
        emptyTitle = "暂无已通过审批",
        emptyBody = "当前没有已通过的审批记录。"
    )
}

private fun String.toApprovalStatusText(): String {
    return when (this) {
        "pending" -> "待处理"
        "approved" -> "已通过"
        "rejected" -> "已拒绝"
        else -> this
    }
}
