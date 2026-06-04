# TabManifest 完整参考

## Android 端数据类

源码位置：client-android/.../open/model/TabManifest.kt

`kotlin
data class TabManifest(
    val id: String,                          // Tab 唯一标识
    val displayName: String,                 // 展示名称，≤ 16 字符
    val description: String?,                // 描述（可选）
    val icon: String?,                       // 图标标识（可选）
    val route: String,                       // 路由，/ 开头
    val entryType: EntryType,                // Native / Web / Hybrid / External
    val entryUri: String?,                   // 入口地址（可选）
    val version: SemanticVersionDto,          // major.minor.patch
    val minContainerVersion: Int,            // 最低容器版本
    val permissions: List<String>,            // 所需权限列表
    val enabled: Boolean,                    // 是否启用
    val sortOrder: Int,                      // 排序
    val extension: TabExtensionDto?,          // 扩展配置（可选）
    val extraConfig: Map<String, String>      // 业务扩展字段
)
`

## 字段约束

| 字段 | 必填 | 类型 | 说明 |
|------|------|------|------|
| id | 是 | String | 反向域名格式，全局唯一 |
| displayName | 是 | String | ≤ 16 字符 |
| route | 是 | String | / 开头 |
| entryType | 是 | EntryType | native/web/hybrid/external |
| version | 是 | SemanticVersionDto | major.minor.patch |
| enabled | 是 | Boolean | 当前用户是否启用 |

## 客户端图标映射

源码位置：client-android/.../open/tab/OpenTabModels.kt

`kotlin
fun iconOf(icon: String?): ImageVector = when (icon) {
    ""approval""   -> Icons.Filled.Sailing
    ""calendar""   -> Icons.Rounded.WbSunny
    ""finance""    -> Icons.Rounded.ColorLens
    ""docs""       -> Icons.Filled.Menu
    ""video""      -> Icons.Filled.SmartDisplay
    ""ai"", ""oncall"" -> Icons.Filled.MoreVert
    else           -> Icons.Filled.Menu
}
`

## 相关来源

api/container-server-api.md → GET /tabs
protocol/tab-register.md