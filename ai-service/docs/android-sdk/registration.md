---
topic: Tab 注册方式（静态代码注册 vs 动态 JSON 配置）
keywords: [注册, registerTab, 静态注册, 动态注册, JSON配置, Kotlin代码]
related_questions:
  - "怎么注册一个 Tab？"
  - "静态注册和动态注册有什么区别？"
  - "什么时候用静态注册？"
  - "JSON 配置怎么注册 Tab？"
  - "动态注册的 JSON 格式是什么？"
  - "两种注册方式能混用吗？"
---

# Tab 注册方式

## 方式一：静态注册（Kotlin 代码）

```kotlin
object ContentAuditTab {
    val definition = TabDefinition(
        id = "com.example.content-audit",
        displayName = "内容审核",
        icon = Icons.Filled.Description,
        route = "/content-audit",
        version = SemanticVersion(1, 0, 0),
        minContainerVersion = 1,
        permissions = listOf("android.permission.INTERNET"),
        extension = TabExtension(
            titleBar = TitleBarExtension(
                menuItems = listOf(
                    MenuItem("filter", "筛选") { /* ... */ },
                    MenuItem("stats", "统计") { /* ... */ }
                )
            )
        )
    )

    @Composable
    fun Page() { /* Composable 页面 */ }

    val lifecycle = object : TabLifecycle {
        override fun onCreate(bundle: Bundle?) { /* 初始化 */ }
        override fun onResume() { /* 刷新 */ }
        override fun onPause() { /* 暂存 */ }
        override fun onDestroy() { /* 清理 */ }
    }
}

// 注册
container.registerTab(
    definition = ContentAuditTab.definition,
    lifecycle = ContentAuditTab.lifecycle,
    page = { ContentAuditTab.Page() }
)
```

优点：类型安全、IDE 代码检查、编译期错误发现。

## 方式二：动态注册（JSON 配置）

```json
{
  "id": "com.example.dashboard",
  "displayName": "数据看板",
  "icon": "dashboard",
  "route": "/dashboard",
  "version": { "major": 1, "minor": 0 },
  "minContainerVersion": 1,
  "permissions": [],
  "extension": {
    "titleBar": { "rightText": "刷新" }
  }
}
```

优点：服务端下发 Tab 列表（下发哪些 Tab 可用、版本号、排序等），无需发版即可调整 Tab。

## 推荐使用方式

| 场景 | 推荐方式 |
|------|---------|
| 固定业务 Tab（如内容审核、数据看板） | **静态注册** |
| 运营动态控制 Tab 显隐/排序 | 动态注册 |
| 服务端 A/B 测试不同 Tab 配置 | 动态注册 |

两者可混用——容器同时支持静态和动态注册的 Tab。

