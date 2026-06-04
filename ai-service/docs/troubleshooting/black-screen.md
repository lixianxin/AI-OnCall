# Android 模拟器黑屏

## 问题现象

Android 模拟器启动后停留在黑屏状态，logcat 显示系统服务正常运行：
- system_server PID 611 正常工作
- ActivityManager、NetworkMonitor、ClipboardListener 均有日志
- 但屏幕无渲染，没有 SurfaceFlinger/Launcher 日志

## 根因

屏幕渲染管线未启动，但系统服务已正常运行。

## 排查步骤

1. 检查 logcat 中有无 SurfaceFlinger/Launcher 崩溃日志
2. 确认 GPU 渲染模式：
   - AVD Manager → 编辑 AVD → Advanced → Graphics
3. 确认模拟器内存配置（建议 ≥ 2GB）

## 解决方案

### 方案1：切换软件渲染（最有效）
`
AVD Manager → 编辑 AVD → Advanced Settings → Graphics → Software - most compatible
`

### 方案2：Wipe Data + Cold Boot
`
AVD Manager → 下拉箭头 → Wipe Data → Cold Boot Now
`

### 方案3：检查 Windows Hyper-V
`
# 以管理员运行 PowerShell
dism /online /enable-feature /featurename:Microsoft-Hyper-V-All
`

## 相关来源

faq/how-to-deploy.md