---
topic: 路由配置与导航方式
keywords: [路由, route, 导航, switchTab, 参数透传, 前缀匹配, startActivity]
related_questions:
  - "route 字段怎么写？"
  - "Tab 之间怎么切换？"
  - "怎么带参数跳转到某个 Tab？"
  - "容器怎么根据 route 分发？"
  - "Tab 内部怎么跳转到独立页面？"
  - "route 重复了会怎样？"
  - "查询参数怎么传给 Tab？"
---

# 路由与导航

## 路由规则

```
容器内路由格式: /{tab-route}?{params}
示例: /content-audit?filter=pending&sort=time
```

- Tab 注册时声明 `route` 前缀（如 `/content-audit`）
- 容器根据 `route` **前缀匹配**分发给对应 Tab
- 查询参数（`?` 之后的部分）由 Tab **自行解析**，容器只透传不处理

## route 字段约束

- 必须以 `/` 开头
- 全局唯一，不可重复
- 建议使用有意义的路径名，如 `/content-audit`、`/dashboard`
- 不要和容器内置路由冲突

## Tab 间切换

```kotlin
// 容器内切换 Tab
container.switchTab(route = "/content-audit")

// 带参数切换
container.switchTab(route = "/content-audit?filter=pending")
```

## Tab 内部跳转独立 Activity

Tab 内如需打开独立页面（如详情页），接入方自行调用 `startActivity`，容器不拦截：

```kotlin
// 在 Tab 的 onClick 中
context.startActivity(Intent(context, DetailActivity::class.java))
```

## Tab 接收路由参数

容器将完整路由透传给 Tab，Tab 在 `onCreate(bundle: Bundle?)` 中解析：

```kotlin
override fun onCreate(bundle: Bundle?) {
    val route = bundle?.getString("route")  // 如 "/content-audit?filter=pending"
    val filter = route?.substringAfter("filter=")?.substringBefore("&")
    loadData(filter)
}
```

