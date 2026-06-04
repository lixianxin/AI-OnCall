---
topic: TabDefinition 字段定义与约束
keywords: [TabDefinition, 元信息, 字段约束, id, displayName, icon, route, version, minContainerVersion, permissions, extension, SemanticVersion]
related_questions:
  - "TabDefinition 有哪些必填字段？"
  - "id 字段的格式要求是什么？"
  - "displayName 最多多少字符？"
  - "icon 的尺寸要求是多少？"
  - "route 字段有什么格式要求？"
  - "minContainerVersion 是干什么的？"
  - "SemanticVersion 的格式是什么？"
  - "permissions 字段怎么填？"
---

# Tab 元信息定义

## TabDefinition 数据结构

```kotlin
@Stable
data class TabDefinition(
    val id: String,                    // 唯一标识，反向域名格式
    val displayName: String,           // 标题栏显示名称
    val icon: ImageVector,             // BottomBar 图标
    val route: String,                 // 路由路径
    val version: SemanticVersion,      // 协议版本号
    val minContainerVersion: Int = 1,  // 最低容器版本
    val permissions: List<String> = emptyList(),  // 所需权限
    val extension: TabExtension? = null  // 扩展点配置
) {
    val fullId: String get() = "$id@$version"
}
```

## SemanticVersion

```kotlin
@Stable
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0
) {
    override fun toString() = "$major.$minor.$patch"
}
```

## 字段约束表

| 字段 | 类型 | 必填 | 默认值 | 约束 |
|------|------|------|--------|------|
| id | String | 是 | - | 反向域名格式（如 `com.example.audit`），全局唯一 |
| displayName | String | 是 | - | ≤ 16 字符，显示在 TitleBar |
| icon | ImageVector | 是 | - | 24dp × 24dp，显示在 BottomBar |
| route | String | 是 | - | `/` 开头（如 `/audit`），用于路由分发 |
| version | SemanticVersion | 是 | - | major.minor.patch，容器据此做兼容处理 |
| minContainerVersion | Int | 否 | 1 | 低于此版本的容器拒绝加载该 Tab |
| permissions | List<String> | 否 | emptyList() | Android 权限字符串，如 `android.permission.CAMERA` |
| extension | TabExtension? | 否 | null | null 则使用容器默认行为（无扩展） |

## fullId 计算属性

`fullId = "$id@$version"`，例如 `"com.example.audit@1.0.0"`，用于唯一标识一个 Tab 的特定版本。
