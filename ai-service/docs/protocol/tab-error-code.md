# Tab 错误码参考

## 错误码列表

| 错误码 | 含义 | 容器行为 |
|--------|------|---------|
| 1001 | Tab ID 重复注册 | 拒绝第二个，Toast 警告 |
| 1002 | minContainerVersion 不满足 | 拒绝加载，提示升级 |
| 1003 | Tab 缺失必填字段 | 拒绝加载 |
| 1004 | 生命周期回调超时（>5s） | 显示兜底页面 |
| 1005 | 生命周期回调抛出异常 | 显示兜底页面 |
| 1006 | Tab 权限不足 | 拒绝加载，提示授权 |

## AI OnCall 项目中的状态映射

Android 端状态定义（client-android/.../open/tab/OpenTabModels.kt）：

`kotlin
OpenTabState.VersionIncompatible → 容器版本 < minContainerVersion
OpenTabState.PermissionDenied   → 用户缺少 tab.xxx.read 权限
OpenTabState.InvalidConfig      → TabManifest 缺少必填字段
OpenTabState.Disabled           → 服务端配置 enabled: false
`

## 权限说明

演示账号权限：

`kotlin
// client-android/.../open/config/OpenApiConfig.kt
object OpenApiConfig {
    const val DEFAULT_ACCOUNT = ""opentab-admin""
    const val DEFAULT_PASSWORD = ""admin123""

    // 该账号拥有权限：
    // tab.approval.read — 审批中心
    // tab.calendar.read — 团队日程
    // tab.finance.read  — 财务看板
    // ai.oncall         — AI 助手
}
`

## 相关来源

protocol/tab-manifest.md → 字段必填约束
api/container-server-api.md → /tabs/validate