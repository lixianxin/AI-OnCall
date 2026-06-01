# 业务 Tab 接入协议 v1.0

> 状态：正式版  
> 负责人：张琦  
> 最后更新：2026-05-30  
> 变更记录：见文档末尾「变更日志」

---

## 一、协议概览

本协议定义业务模块接入客户端容器的标准化方式。接入方只需按协议声明 Tab 元信息、实现生命周期回调、配置扩展点，即可将业务页面嵌入容器，无需修改容器核心代码。

### 核心参与者

```
┌─────────────────┐       ┌─────────────────┐
│   业务 Tab       │ ───→  │   容器 (主动方)  │
│  (接入方/被动方)  │ ←───  │                 │
└─────────────────┘       └─────────────────┘
```

- **容器**：负责 Tab 注册管理、页面切换、TitleBar 渲染、生命周期调度
- **接入方**：实现协议接口，提供页面 Composable、响应容器生命周期

---

## 二、Tab 元信息

### 2.1 TabDefinition

```kotlin
@Stable
data class TabDefinition(
    val id: String,                    // 唯一标识，如 "com.example.content-audit"
    val displayName: String,           // 标题栏显示名称
    val icon: ImageVector,             // BottomBar 图标
    val route: String,                 // 路由路径，如 "/content-audit"
    val version: SemanticVersion,      // 协议版本
    val minContainerVersion: Int,      // 最低容器版本
    val permissions: List<String>,     // 所需权限（网络、存储等）
    val extension: TabExtension?       // 扩展点配置
) {
    val fullId: String get() = "$id@$version"
}

@Stable
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0
) {
    override fun toString() = "$major.$minor.$patch"
}
```

### 2.2 字段约束

| 字段 | 必填 | 说明 |
|------|------|------|
| id | 是 | 反向域名格式，全局唯一 |
| displayName | 是 | ≤ 16 字符，容器 TitleBar 用 |
| icon | 是 | 24dp × 24dp |
| route | 是 | `/` 开头，用于路由分发 |
| version | 是 | 协议版本号，容器据此做兼容处理 |
| minContainerVersion | 否 | 默认 1，低于此版本容器拒绝加载 |
| permissions | 否 | 如 `["android.permission.CAMERA"]` |
| extension | 否 | null 则使用容器默认行为 |

---

## 三、生命周期

### 3.1 回调定义

```kotlin
interface TabLifecycle {
    fun onCreate(bundle: Bundle?)                    // Tab 首次创建
    fun onResume()                                   // Tab 可见（切换回来）
    fun onPause()                                    // Tab 不可见（切换走）
    fun onDestroy()                                  // Tab 被销毁
    fun onConfigChange(config: Configuration) = {}   // 配置变更（横竖屏等），可选
}
```

### 3.2 生命周期状态机

```
Tab 创建
  │
  ▼
onCreate ──→ onResume
                │
   ◄────────────┘ (Tab 切换)
                │
                ▼
            onPause
                │
          ┌─────┴─────┐
          ▼           ▼
      onResume    onDestroy
      (切回来)      (移除 Tab)
```

### 3.3 调度规则

1. **首次进入**：onCreate → onResume
2. **Tab 间切换**：当前 Tab.onPause → 新 Tab.onResume（若新 Tab 首次则先 onCreate）
3. **退出 Tab**：onPause → onDestroy
4. **超时保护**：每个回调执行上限 5 秒，超时容器自动降级处理（显示兜底页面）
5. **异常保护**：回调抛异常不影响其他 Tab

---

## 四、UI 扩展点

### 4.1 扩展点类型

```kotlin
data class TabExtension(
    val titleBar: TitleBarExtension?,    // TitleBar 右侧按钮/菜单
    val fab: FabExtension?,              // 悬浮按钮
    val bottomPanel: BottomPanel?        // 底部扩展面板
)

data class TitleBarExtension(
    val rightIcon: ImageVector?,         // 右侧单个图标按钮
    val rightText: String?,              // 右侧文字按钮
    val menuItems: List<MenuItem> = emptyList()  // 右侧溢出菜单（三点）
) {
    // rightIcon 和 rightText/menuItems 互斥：rightIcon 不为 null 时，忽略 menuItems
}

data class MenuItem(
    val id: String,
    val label: String,
    val onClick: () -> Unit
)

data class FabExtension(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

data class BottomPanel(
    val content: @Composable () -> Unit,
    val defaultHeight: Int  // 默认面板高度 dp
)
```

### 4.2 扩展点行为

| 扩展点 | 默认行为 | 自定义后 |
|--------|---------|---------|
| TitleBar 右侧 | 无按钮 | 显示接入方指定的图标/文字/菜单 |
| TitleBar 标题 | `TabDefinition.displayName` | 通过回调实时更新 |
| FAB | 无 | 右下角悬浮按钮 |
| BottomPanel | 无 | v2.0 预留，v1.0 容器忽略此配置 |

### 4.3 v1.0 范围与后续版本预留

**v1.0 明确支持：**
- TitleBar 右侧按钮/菜单
- FAB 悬浮按钮

**v1.0 明确不支持（v2.0 候选）：**
- BottomPanel 底部扩展面板
- BottomBar 动态增加 Tab（当前容器只支持预定义的 MainPageTab 枚举）
- 自定义 Drawer 内容
- Tab 间通信 / 数据共享
- 图片预览、用户选择等通用能力（由接入方自行实现）

---

## 五、配置格式

### 5.1 静态注册（代码方式）

```kotlin
// 接入方在自己的模块中实现
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
                    MenuItem("filter", "筛选", { /* ... */ }),
                    MenuItem("stats", "统计", { /* ... */ })
                )
            )
        )
    )

    @Composable
    fun Page() {
        // 接入方的 Composable 页面
    }

    val lifecycle = object : TabLifecycle {
        override fun onCreate(bundle: Bundle?) { /* 初始化 */ }
        override fun onResume() { /* 显示时刷新数据 */ }
        override fun onPause() { /* 保存状态 */ }
        override fun onDestroy() { /* 清理资源 */ }
    }
}
```

### 5.2 动态注册（JSON 配置）

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
    "titleBar": {
      "rightText": "刷新"
    }
  }
}
```

### 5.3 推荐方式

- **静态注册**：接入方的页面和逻辑用 Kotlin 代码实现（类型安全、IDE 检查）
- **动态注册**：作为静态注册的补充，用于服务端下发的 Tab 列表（下发哪些 Tab 可用、版本号、排序等）

---

## 六、路由

### 6.1 路由规则

```
容器内路由格式: /{tab-route}?{params}
示例: /content-audit?filter=pending
```

- Tab 注册时声明 `route` 前缀
- 容器根据 `route` 前缀匹配分发到对应 Tab
- 查询参数由 Tab 自行解析（容器透传）

### 6.2 导航方式

```kotlin
// 容器内 Tab 间切换
container.switchTab(route = "/content-audit")

// Tab 内部跳转独立 Activity（如详情页）
// 由接入方自行 startActivity，容器不拦截
```

---

## 七、兼容策略

### 7.1 版本号语义

```
major.minor.patch

major: 不兼容变更（容器必须升级才能加载）
minor: 新增可选字段/回调（老容器忽略即可）
patch: 文档修正、Bug 描述（无行为变化）
```

### 7.2 向后兼容规则

| 场景 | 容器行为 |
|------|---------|
| 容器版本 < Tab.minContainerVersion | 拒绝加载，Toast 提示 "请升级客户端" |
| Tab 发送了容器不认识的字段 | 容器忽略多余字段 |
| 容器新增字段，Tab 未提供 | 容器使用默认值 |
| Tab 调用不存在的扩展点 | 容器静默忽略 |

### 7.3 废弃流程

```
v1.x 标记 @Deprecated → v2.0 正式移除

示例：
字段 oldName → 保留两个版本，v1.2 标记 deprecated，v2.0 删除
```

---

## 八、错误码

| 错误码 | 含义 | 容器行为 |
|--------|------|---------|
| 1001 | Tab ID 重复注册 | 拒绝第二个，Toast 警告 |
| 1002 | minContainerVersion 不满足 | 拒绝加载，提示升级 |
| 1003 | Tab 缺失必填字段 | 拒绝加载 |
| 1004 | 生命周期回调超时（>5s） | 显示兜底页面 |
| 1005 | 生命周期回调抛出异常 | 显示兜底页面，打印日志 |
| 1006 | Tab 权限不足 | 拒绝加载，提示用户授权 |

---

## 九、接入流程（三步概览）

```
第一步：实现 TabLifecycle 接口，提供 Composable 页面
第二步：声明 TabDefinition 元信息
第三步：调用 Container.registerTab(definition, lifecycle, page)
```

完整示例代码、详细配置说明、常见错误排查见 `docs/dev-guide.md`。

---

## 十、v1.0 范围决议

以下议题经团队讨论已形成决议：

| 议题 | 决议 | 说明 |
|------|------|------|
| BottomBar 动态增加 Tab | **v1.0 不支持** | 当前容器使用 MainPageTab 枚举，动态增加需容器侧架构调整，纳入 v2.0 |
| Tab 间数据共享 | **v1.0 不支持** | 各 Tab 保持独立。如需共享数据，通过宿主 Activity/ViewModel 显式传参 |
| BottomPanel 扩展面板 | **v1.0 不支持** | 需求场景尚不明确，标记为 v2.0 候选 |
| 通用能力（图片预览、用户选择等） | **不纳入协议** | 此类能力与协议无关，由接入方自行实现或作为独立工具库提供 |

---

## 十一、变更日志

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-05-30 | 正式发布。明确 v1.0 范围：TitleBar/FAB 扩展点、静态+动态注册、6 个错误码、5 条调度规则 |
| v1.0-draft | 2026-05-28 | 初始草案，待团队 review |

---

## 十二、附录

### 相关文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 开发者接入指南 | `docs/dev-guide.md` | 三步接入教程、完整代码示例、排查清单 |
| 知识库（RAG 检索用） | `docs/knowledge-base/` | 9 个结构化知识片段 + 55 条典型问答对 + 检索 Prompt 模板 |

### 协议接口代码

协议定义的 Kotlin 类型（TabDefinition、TabLifecycle、TabExtension 等）实现文件位于：

```
client/app/src/main/java/github/leavesczy/compose_chat/protocol/
```

具体代码文件列表见该目录。
