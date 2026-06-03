package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.SemanticVersionDto
import github.leavesczy.compose_chat.open.model.TabExtensionDto
import github.leavesczy.compose_chat.open.model.TabManifest
import github.leavesczy.compose_chat.open.model.TitleBarExtensionDto

object OpenMockData {

    val defaultPermissions = setOf(
        "tab.approval.read",
        "tab.calendar.read",
        "ai.oncall"
    )

    fun tabs(): List<TabManifest> {
        return listOf(
            tab(
                id = "approval",
                displayName = "审批",
                description = "查看待处理和已完成的审批事项。",
                icon = "approval",
                route = "/approval",
                entryType = EntryType.Native,
                permissions = listOf("tab.approval.read"),
                sortOrder = 10,
                rightText = "刷新"
            ),
            tab(
                id = "calendar",
                displayName = "日程",
                description = "查看今天的会议和团队日程。",
                icon = "calendar",
                route = "/calendar",
                entryType = EntryType.Native,
                permissions = listOf("tab.calendar.read"),
                sortOrder = 20
            ),
            tab(
                id = "finance",
                displayName = "财务",
                description = "用于演示权限不足状态的业务页。",
                icon = "finance",
                route = "/finance",
                entryType = EntryType.Native,
                permissions = listOf("tab.finance.read"),
                sortOrder = 30
            ),
            tab(
                id = "docs",
                displayName = "文档",
                description = "用于演示 Web Tab 的占位页面。",
                icon = "docs",
                route = "/docs",
                entryType = EntryType.Web,
                entryUri = "https://example.com/opentab/docs",
                permissions = emptyList(),
                sortOrder = 40
            ),
            bilibiliWebTab(),
            tiktokShortVideoTab(),
            tab(
                id = "ai-oncall",
                displayName = "AI oncall",
                description = "提供协议问答、配置诊断和接入建议。",
                icon = "ai-oncall",
                route = "/ai-oncall",
                entryType = EntryType.Native,
                permissions = listOf("ai.oncall"),
                sortOrder = 50
            ),
            tab(
                id = "legacy-hybrid",
                displayName = "混合",
                description = "用于演示暂不支持的混合入口类型。",
                icon = "dashboard",
                route = "/legacy-hybrid",
                entryType = EntryType.Hybrid,
                permissions = emptyList(),
                sortOrder = 60
            ),
            tab(
                id = "future-tab",
                displayName = "未来功能",
                description = "用于演示容器版本不兼容状态。",
                icon = "dashboard",
                route = "/future",
                entryType = EntryType.Native,
                permissions = emptyList(),
                minContainerVersion = 2,
                sortOrder = 70
            )
        )
    }

    fun bilibiliWebTab(): TabManifest {
        return tab(
            id = "bilibili-web",
            displayName = "B站首页",
            description = "通过 WebView 接入哔哩哔哩移动端首页，演示用户自定义网页 Tab。",
            icon = "video",
            route = "/bilibili",
            entryType = EntryType.Web,
            entryUri = "https://m.bilibili.com",
            permissions = emptyList(),
            sortOrder = 45
        )
    }

    fun tiktokShortVideoTab(): TabManifest {
        return tab(
            id = "tiktok-short-video",
            displayName = "TikTok 短视频",
            description = "通过 Web 页面容器接入 TikTok 网页，用于演示短视频滑动浏览和基础播放。",
            icon = "video",
            route = "/tiktok",
            entryType = EntryType.Web,
            entryUri = "https://www.tiktok.com/zh-Hans",
            permissions = emptyList(),
            sortOrder = 48,
            extraConfig = mapOf(
                "containerType" to "web",
                "category" to "leisure",
                "allowedHosts" to "tiktok.com",
                "fallbackAsset" to "short_video_fallback.html"
            )
        )
    }

    fun aiOncallTab(): TabManifest {
        return tab(
            id = "ai-oncall",
            displayName = "AI oncall",
            description = "提供协议问答、配置诊断和接入建议。",
            icon = "ai-oncall",
            route = "/ai-oncall",
            entryType = EntryType.Native,
            permissions = listOf("ai.oncall"),
            sortOrder = 50
        )
    }

    private fun tab(
        id: String,
        displayName: String,
        description: String,
        icon: String,
        route: String,
        entryType: EntryType,
        permissions: List<String>,
        sortOrder: Int,
        entryUri: String? = null,
        minContainerVersion: Int = 1,
        rightText: String? = null,
        extraConfig: Map<String, String> = emptyMap()
    ): TabManifest {
        return TabManifest(
            id = id,
            displayName = displayName,
            description = description,
            icon = icon,
            route = route,
            entryType = entryType,
            entryUri = entryUri,
            version = SemanticVersionDto(major = 1, minor = 0, patch = 0),
            minContainerVersion = minContainerVersion,
            permissions = permissions,
            enabled = true,
            sortOrder = sortOrder,
            extension = TabExtensionDto(
                titleBar = TitleBarExtensionDto(
                    rightText = rightText,
                    menuItems = emptyList()
                ),
                fab = null
            ),
            extraConfig = extraConfig
        )
    }

}
