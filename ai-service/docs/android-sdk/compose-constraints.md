---
topic: Compose 集成约束
keywords: [Compose, Scaffold, TopBar, WindowInsets, Modifier, LazyColumn, 导航, 主题, 组件限制]
related_questions:
  - "Tab 的 Composable 能不能包含 Scaffold？"
  - "为什么不能自己设置 WindowInsets？"
  - "Tab 里能用 LazyColumn 吗？"
  - "Tab 的 Modifier 有什么限制？"
  - "容器提供的主题我的页面能用吗？"
  - "Tab 里能用 Navigation 组件吗？"
---

# Compose 集成约束

## 禁止使用的组件

| 组件 | 原因 | 替代方案 |
|------|------|---------|
| `Scaffold` | 容器已提供，双层 Scaffold 导致布局错乱 | 直接使用 `Column`/`Box`/`LazyColumn` 等布局 |
| `TopAppBar` / `CenterAlignedTopAppBar` | 容器统一管理 TitleBar | 通过 `TabExtension.titleBar` 配置右侧按钮 |
| `NavigationBar` / `BottomAppBar` | 容器统一管理 BottomBar | 暂不支持自定义 BottomBar，v2.0 规划 |
| `ModalNavigationDrawer` | 容器统一管理 Drawer | 暂不支持自定义 Drawer，v2.0 规划 |

## WindowInsets

```kotlin
// 不要在 Tab Composable 里设置 WindowInsets
// 容器已经处理了状态栏、导航栏的安全区域

// 错误做法：
@Composable
fun MyTabPage() {
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) { ... }
    }
}

// 正确做法：
@Composable
fun MyTabPage() {
    // 直接布局，无需关心 insets
    LazyColumn {
        items(list) { ... }
    }
}
```

## 可以正常使用的组件

以下是安全可用、无限制的组件：

- `LazyColumn` / `LazyRow` / `LazyVerticalGrid`
- `Column` / `Row` / `Box` / `FlowRow`
- `TextField` / `OutlinedTextField`
- `Button` / `TextButton` / `IconButton` / `FloatingActionButton`
- `Card` / `ListItem` / `AlertDialog`
- `Text` / `Image` / `Icon`
- `TabRow` / `HorizontalPager`
- `SwipeToDismiss` / `PullToRefresh`
- `AnimatedVisibility` / `AnimatedContent`
- Material3 颜色、字体、形状（继承容器主题）

## Modifier 使用规范

```kotlin
@Composable
fun MyTabPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()     // 填充容器分配的空间
            .padding(16.dp)    // 内边距
    ) {
        Text("标题")
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)    // 占据剩余空间
        ) {
            items(items) { ... }
        }
    }
}
```

## 主题与样式

Tab 的 Composable 自动继承容器的 MaterialTheme：

```kotlin
@Composable
fun MyTabPage() {
    // 直接使用 MaterialTheme，无需重新包裹
    Text(
        text = "标题",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
```

## Tab 内部页面跳转

```kotlin
// Tab 内部跳转到详情页——使用独立 Activity
@Composable
fun MyTabPage(context: Context, viewModel: MyViewModel) {
    LazyColumn {
        items(viewModel.items) { item ->
            Card(onClick = {
                context.startActivity(
                    Intent(context, DetailActivity::class.java).apply {
                        putExtra("item_id", item.id)
                    }
                )
            }) {
                Text(item.title)
            }
        }
    }
}

// 不要使用 Navigation Compose 在 Tab 内部做子路由
// v1.0 容器不支持嵌套 NavHost
```

