# 生成 Compose 页面 — AI OnCall 项目风格

本项目使用 Jetpack Compose + Material3，遵循 OpenTab 容器设计规范。

## 参考代码

`kotlin
// client-android/.../open/ui/OpenOnCallPage.kt
@Composable
fun OpenOnCallPage(
    modifier: Modifier = Modifier,
    repository: OpenOnCallRepository = remember { OpenOnCallRepository() }
) {
    val messages = remember { mutableStateListOf<OnCallMessageUi>() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        // 消息列表
        LazyColumn { ... }
        // 输入栏
        OnCallInputBar(value = input, onSend = { ... })
    }
}
`

## 配色规范

| 用途 | 色值 |
|------|------|
| 页面背景 | #F6F8FB |
| 主色调 / 按钮 | #2563EB |
| 用户气泡 | #2563EB（蓝底白字）|
| AI 气泡 | #FFFFFF（白底深灰字）|
| 工具卡片 | #FFF7ED（浅橙）|
| 错误文字 | #B91C1C |
| 正文 | #1F2937 / #111827 |
| 辅助文字 | #6B7280 |

## 圆角规范

| 元素 | 圆角 |
|------|------|
| 消息气泡 | 18dp（发送/接收角 4dp）|
| 卡片 | 14dp |
| 输入框 | 18dp |
| 发送按钮 | 50%（圆形）|
| 快捷问题 | 999dp（胶囊）|

## 生成模板

`kotlin
@Composable
fun Page(
    modifier: Modifier = Modifier,
    viewModel: ViewModel = remember { ViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
            .padding(16.dp)
    ) {
        Text(
            text = """",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 内容区域
    }
}
`

## 相关来源

architecture/system-overview.md → OpenOnCallPage.kt