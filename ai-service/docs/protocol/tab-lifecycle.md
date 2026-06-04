# Tab 生命周期

## 接口定义

`kotlin
interface TabLifecycle {
    fun onCreate(bundle: Bundle?)                     // Tab 首次创建，初始化数据
    fun onResume()                                    // Tab 可见，刷新页面
    fun onPause()                                     // Tab 不可见，保存状态
    fun onDestroy()                                   // Tab 被销毁，释放资源
    fun onConfigChange(config: Configuration) = {}    // 可选：横竖屏切换
}
`

## 调度规则

1. 首次进入：onCreate → onResume
2. Tab 切换：当前 onPause → 新 Tab onResume
3. 退出 Tab：onPause → onDestroy
4. 超时保护：每个回调上限 5 秒

## AI OnCall 页面接入示例

`kotlin
// 在工作台页面中嵌入 AI OnCall
// 源码：client-android/.../open/ui/OpenTabContentHost.kt

// OnCallPlaceholderPage 内部调用：
OpenOnCallPage(
    modifier = Modifier.fillMaxSize(),
    repository = remember { OpenOnCallRepository() }
)
`

## 相关来源

protocol/tab-register.md
protocol/tab-error-code.md