package github.leavesczy.compose_chat.open.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.ui.theme.AppTheme
import github.leavesczy.compose_chat.ui.widgets.CommonButton

@Composable
fun OpenTabContentHost(
    modifier: Modifier = Modifier,
    tab: OpenTabItem?
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
            message = tab.openState.toDisplayMessage(tab = tab)
        )
        return
    }
    when (tab.manifest.entryType) {
        EntryType.Native -> NativeTabPage(modifier = modifier, tab = tab)
        EntryType.Web -> WebTabPlaceholderPage(modifier = modifier, tab = tab)
        EntryType.External -> ExternalTabPlaceholderPage(modifier = modifier, tab = tab)
        EntryType.Hybrid,
        EntryType.Unknown -> OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = "当前客户端暂不支持 ${tab.manifest.entryType} 入口类型。"
        )
    }
}

@Composable
private fun NativeTabPage(
    modifier: Modifier,
    tab: OpenTabItem
) {
    when (tab.manifest.route) {
        "/approval" -> ApprovalPlaceholderPage(modifier = modifier, tab = tab)
        "/calendar" -> CalendarPlaceholderPage(modifier = modifier, tab = tab)
        "/finance" -> FinancePlaceholderPage(modifier = modifier, tab = tab)
        "/ai-oncall" -> OnCallPlaceholderPage(modifier = modifier, tab = tab)
        else -> OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = "客户端尚未注册 ${tab.manifest.route} 对应的原生页面。"
        )
    }
}

@Composable
private fun ApprovalPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem
) {
    OpenTabScaffold(modifier = modifier, tab = tab) {
        MetricRow(label = "待处理审批", value = "12")
        MetricRow(label = "今日已通过", value = "8")
        SectionCard(
            title = "采购申请",
            body = "申请人：张三\n状态：待处理\n创建时间：2026-05-30 10:00"
        )
    }
}

@Composable
private fun CalendarPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem
) {
    OpenTabScaffold(modifier = modifier, tab = tab) {
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
    tab: OpenTabItem
) {
    OpenTabScaffold(modifier = modifier, tab = tab) {
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
    OpenTabScaffold(modifier = modifier, tab = tab) {
        SectionCard(
            title = "AI oncall",
            body = "第一阶段先提供页面壳。消息列表、输入区和 /oncall/stream SSE 联调将在第二阶段接入。"
        )
        CommonButton(
            modifier = Modifier
                .padding(top = 4.dp),
            text = "SSE 流式接口预留",
            onClick = {}
        )
    }
}

@Composable
private fun WebTabPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem
) {
    OpenTabScaffold(modifier = modifier, tab = tab) {
        SectionCard(
            title = "Web Tab 占位页",
            body = "entryUri：${tab.manifest.entryUri.orEmpty()}\n第一阶段先展示占位内容，后续可接入 WebView 或外部浏览器。"
        )
    }
}

@Composable
private fun ExternalTabPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem
) {
    OpenTabScaffold(modifier = modifier, tab = tab) {
        SectionCard(
            title = "外部入口占位页",
            body = "entryUri：${tab.manifest.entryUri.orEmpty()}\n第一阶段不会直接拉起外部应用。"
        )
    }
}

@Composable
private fun OpenTabScaffold(
    modifier: Modifier,
    tab: OpenTabItem,
    content: @Composable ColumnScope.() -> Unit
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
        Text(
            modifier = Modifier,
            text = tab.displayName,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
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
private fun OpenStatePage(
    modifier: Modifier,
    title: String,
    message: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
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
        OpenTabState.VersionIncompatible -> "当前容器版本过低。要求版本：${tab.manifest.minContainerVersion}，当前版本：1。"
        OpenTabState.RouteUnsupported -> "客户端尚未注册 ${tab.manifest.route} 对应的页面。"
        OpenTabState.EntryUnsupported -> "当前客户端暂不支持 ${tab.manifest.entryType} 入口类型。"
        OpenTabState.InvalidConfig -> "服务端下发的 Tab 配置缺少必填字段。"
    }
}
