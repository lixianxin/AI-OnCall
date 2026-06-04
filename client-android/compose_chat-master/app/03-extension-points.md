---
topic: UI 扩展点配置（TitleBar、FAB、BottomPanel）
keywords: [TabExtension, TitleBarExtension, FabExtension, BottomPanel, 扩展点, rightIcon, rightText, menuItems, 互斥规则, 溢出菜单]
related_questions:
  - "有哪些扩展点可以配置？"
  - "TitleBar 右侧怎么加按钮？"
  - "rightIcon 和 menuItems 能同时显示吗？"
  - "怎么加悬浮按钮 FAB？"
  - "BottomPanel 是什么？"
  - "扩展点不配置会怎样？"
  - "MenuItem 怎么定义？"
  - "哪些扩展点本期不支持？"
---

# UI 扩展点

## 扩展点总览

```kotlin
data class TabExtension(
    val titleBar: TitleBarExtension?,    // TitleBar 右侧按钮/菜单
    val fab: FabExtension?,              // 悬浮按钮
    val bottomPanel: BottomPanel?        // 底部扩展面板
)
```

三个扩展点**全部可选**，不传则使用容器默认行为（无扩展）。

## TitleBar 扩展

```kotlin
data class TitleBarExtension(
    val rightIcon: ImageVector?,         // 右侧单个图标按钮
    val rightText: String?,              // 右侧文字按钮
    val menuItems: List<MenuItem> = emptyList()  // 右侧溢出菜单（三点）
)
```

### 互斥规则（重要）

**rightIcon 和 menuItems 互斥**：当 `rightIcon` 不为 null 时，`menuItems` 被忽略。只能二选一。

| 配置方式 | 效果 | 适用场景 |
|---------|------|---------|
| rightIcon = 图标 | TitleBar 右侧显示单个图标按钮 | 只有一个操作（如筛选） |
| menuItems = [...] | TitleBar 右侧显示三点溢出菜单 | 有多个操作（如筛选、导出、统计） |
| rightText = "文字" | TitleBar 右侧显示文字按钮 | 操作需要文字说明 |
| 都不配置 | TitleBar 右侧无内容 | 无额外操作 |

### MenuItem 定义

```kotlin
data class MenuItem(
    val id: String,
    val label: String,
    val onClick: () -> Unit
)
```

## FAB 扩展（悬浮按钮）

```kotlin
data class FabExtension(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)
```

显示在页面右下角，如"新建""搜索"等高频操作。

## BottomPanel 扩展（底部面板）

```kotlin
data class BottomPanel(
    val content: @Composable () -> Unit,
    val defaultHeight: Int  // 默认面板高度 dp
)
```

页面底部可展开/收起的扩展面板。

## 各扩展点行为对照

| 扩展点 | 不配置（默认） | 配置后 |
|--------|--------------|--------|
| TitleBar 右侧 | 无按钮 | 显示图标/文字/菜单 |
| TitleBar 标题 | displayName | 可通过回调实时更新 |
| FAB | 无 | 右下角悬浮按钮 |
| BottomPanel | 无 | 页面底部扩展面板 |

## 本期不支持（已预留）

- BottomBar 动态增加 Tab（当前只支持预定义枚举 MainPageTab）
- 自定义 Drawer 内容
- Tab 间通信 / 数据共享
