# 如何注册一个业务 Tab

## 项目注册流程

在 AI OnCall 项目中，注册 Tab 分为服务端下发和客户端渲染两步：

### 1. 服务端下发 TabManifest

`json
{
  ""id"": ""approval"",
  ""displayName"": ""审批中心"",
  ""route"": ""/approval"",
  ""entryType"": ""native"",
  ""entryUri"": ""native://approval"",
  ""version"": { ""major"": 1, ""minor"": 0, ""patch"": 0 },
  ""permissions"": [""tab.approval.read""],
  ""enabled"": true,
  ""sortOrder"": 10
}
`

接口：GET /tabs（Container Server Mock，端口 8080）

### 2. 客户端解析与渲染

Android 端 OpenTabRepository 获取 TabManifest 列表后：
1. 检查 entryType → NativeTabPage / WebTabPage
2. 检查 route → 匹配到具体页面
3. 检查权限和版本 → 确定 OpenTabState

`kotlin
// OpenTabContentHost.kt
when (tab.manifest.route) {
    ""/approval""  -> 审批页面
    ""/calendar""  -> 日程页面
    ""/finance""   -> 财务页面
    ""/ai-oncall"" -> AI OnCall 聊天
}
`

## 新增一个业务 Tab 需要改什么

1. 服务端：在 /tabs 返回值中添加新的 TabManifest
2. Android：在 OpenTabRegistry.nativeRoutes 中添加路由
3. Android：在 OpenTabContentHost 中添加对应页面

## 相关来源

protocol/tab-manifest.md
protocol/tab-register.md