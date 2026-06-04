# 注册 Tab — AI OnCall 项目实现

## Android 端路由注册表

源码位置：client-android/.../open/tab/OpenTabModels.kt

`kotlin
object OpenTabRegistry {
    // 已注册的 Native 路由
    private val nativeRoutes = setOf(
        ""/approval"",
        ""/calendar"",
        ""/finance"",
        ""/ai-oncall""
    )

    fun supportsRoute(route: String): Boolean {
        return route in nativeRoutes || route == ""/docs"" 
            || route == ""/bilibili"" || route.startsWith(""/custom-"")
    }

    fun supportsEntryType(entryType: EntryType): Boolean {
        return entryType == EntryType.Native 
            || entryType == EntryType.Web 
            || entryType == EntryType.External
    }
}
`

## Tab 内容分发

源码位置：client-android/.../open/ui/OpenTabContentHost.kt

`kotlin
when (tab.manifest.entryType) {
    EntryType.Native -> NativeTabPage(tab = tab)
    EntryType.Web -> WebTabPlaceholderPage(tab = tab)
    EntryType.External -> ExternalTabPlaceholderPage(tab = tab)
    EntryType.Hybrid, EntryType.Unknown -> OpenStatePage(...)
}
`

Native 路由分发：

`kotlin
when (tab.manifest.route) {
    ""/approval""  -> ApprovalPlaceholderPage()
    ""/calendar""  -> CalendarPlaceholderPage()
    ""/finance""   -> FinancePlaceholderPage()
    ""/ai-oncall"" -> OnCallPlaceholderPage() // 嵌入 AI OnCall 聊天
    else           -> OpenStatePage()
}
`

## 状态检查

源码位置：client-android/.../open/tab/OpenTabModels.kt

`kotlin
enum class OpenTabState {
    Openable,              // 可打开
    Disabled,              // 服务端禁用
    PermissionDenied,      // 权限不足
    VersionIncompatible,   // 容器版本过低
    RouteUnsupported,      // 路由不支持
    EntryUnsupported,      // 入口类型不支持
    InvalidConfig          // 配置异常
}
`

## 相关来源

protocol/tab-manifest.md
protocol/tab-lifecycle.md