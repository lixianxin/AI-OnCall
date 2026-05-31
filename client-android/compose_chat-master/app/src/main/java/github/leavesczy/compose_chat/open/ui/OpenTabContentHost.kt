package github.leavesczy.compose_chat.open.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import github.leavesczy.compose_chat.open.model.ApprovalSummaryResponse
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OpenTabRepository
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.ui.theme.AppTheme

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
            message = "当前客户端暂不支持 ${tab.manifest.entryType} 入口类型。",
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
        "/ai-oncall" -> OnCallPlaceholderPage(modifier = modifier, tab = tab)
        else -> OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = "客户端尚未注册 ${tab.manifest.route} 对应的原生页面。",
            onBackToWorkbench = onBackToWorkbench
        )
    }
}

@Composable
private fun ApprovalPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    repository: OpenTabRepository = remember { OpenTabRepository() }
) {
    var summary by remember { mutableStateOf<ApprovalSummaryResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        loading = true
        when (val result = repository.getApprovalSummary()) {
            is OpenApiResult.Success -> {
                summary = result.data
                errorMessage = null
            }

            is OpenApiResult.Failed -> {
                errorMessage = result.message
            }
        }
        loading = false
    }
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        if (loading) {
            SectionCard(title = "审批数据", body = "正在从服务端加载审批摘要…")
        }
        if (errorMessage != null) {
            SectionCard(title = "审批数据加载失败", body = errorMessage.orEmpty())
        }
        val data = summary
        if (data != null) {
            MetricRow(label = "待处理审批", value = data.pendingCount.toString())
            MetricRow(label = "今日已通过", value = data.approvedToday.toString())
            data.items.forEach { item ->
                SectionCard(
                    title = item.title,
                    body = "申请人：${item.applicant}\n状态：${item.status.toApprovalStatusText()}\n创建时间：${item.createdAt}"
                )
            }
        }
    }
}

@Composable
private fun CalendarPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        MetricRow(label = "今日日程", value = "3")
        SectionCard(
            title = "项目周会",
            body = "时间：14:00 - 15:00\n地点：线上会议\n第一阶段使用本地 Mock 数据展示。"
        )
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
            body = "该页面用于演示权限控制。默认演示权限不包含 tab.finance.read，因此会展示权限不足状态。"
        )
    }
}

@Composable
private fun OnCallPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem
) {
    OpenOnCallPage(modifier = modifier)
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
            title = "外部入口占位页",
            body = "entryUri：${tab.manifest.entryUri.orEmpty()}\n第一阶段不会直接拉起外部应用。"
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
            Text(
                text = entryUri.ifBlank { "未配置网页地址" },
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
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
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
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
                    body = "${errorMessage.orEmpty()}\n地址：$entryUri"
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding()
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        OpenTabHeader(tab = tab, onBackToWorkbench = onBackToWorkbench)
        Text(
            modifier = Modifier,
            text = tab.manifest.description.orEmpty(),
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        SectionCard(
            title = "Tab 配置",
            body = "id=${tab.manifest.id}\nroute=${tab.manifest.route}\nentryType=${tab.manifest.entryType}\nminContainerVersion=${tab.manifest.minContainerVersion}"
        )
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
            IconButton(
                modifier = Modifier.size(size = 40.dp),
                onClick = onBackToWorkbench
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回工作台",
                    tint = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
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
                text = "由业务方提供内容，容器负责承载",
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
            IconButton(onClick = onBackToWorkbench) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回工作台",
                    tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
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
        OpenTabState.Disabled -> "该业务 Tab 已被服务端配置为禁用。"
        OpenTabState.PermissionDenied -> "权限不足，缺少权限：${tab.manifest.permissions.joinToString()}"
        OpenTabState.VersionIncompatible -> "当前容器版本为 1，${tab.displayName} 要求容器版本不低于 ${tab.manifest.minContainerVersion}。这是开放式 Tab 容器的兼容性保护：客户端不会强行打开高版本业务页，避免入口协议或页面能力不匹配。"
        OpenTabState.RouteUnsupported -> "客户端尚未注册 ${tab.manifest.route} 对应的页面。"
        OpenTabState.EntryUnsupported -> "当前客户端暂不支持 ${tab.manifest.entryType} 入口类型。"
        OpenTabState.InvalidConfig -> "服务端下发的 Tab 配置缺少必填字段。"
    }
}

private fun String.toApprovalStatusText(): String {
    return when (this) {
        "pending" -> "待处理"
        "approved" -> "已通过"
        "rejected" -> "已拒绝"
        else -> this
    }
}
