# 服务端接口联调记录（2026-05-31）

## 1. 基本信息

| 项目 | 内容 |
|---|---|
| 测试时间 | 2026-05-31 14:29 左右（UTC+08:00） |
| 测试环境 | Windows PowerShell |
| 服务地址 | `http://121.40.241.161:8080` |
| 服务模式 | `mock` |
| 测试目的 | 确认当前云端 Mock 服务已实现接口、未实现接口，以及客户端下一阶段开发边界 |

> 说明：PowerShell 当前控制台存在中文编码显示问题，部分中文响应在终端中显示为乱码；接口本身可以正常返回 JSON，客户端侧按 UTF-8 解析即可。

---

## 2. 已验证通过的接口

### 2.1 健康检查

```powershell
curl.exe "http://121.40.241.161:8080/health"
```

实际结果：

```json
{
  "mode": "mock",
  "serverTime": "2026-05-31T14:29:16+08:00",
  "service": "tab-container-server",
  "status": "ok"
}
```

结论：接口可用，服务处于 Mock 模式。

---

### 2.2 管理员登录

```powershell
$body = @{ account = "opentab-admin"; password = "admin123" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "http://121.40.241.161:8080/auth/login" -Method Post -ContentType "application/json" -Body $body
$login
```

实际结果摘要：

```text
token: mock-admin-token
userId: user-admin
displayName: OpenTab 管理员
permissions: tab.approval.read, tab.calendar.read, tab.finance.read, ai.oncall
```

结论：管理员账号登录成功。

---

### 2.3 当前用户信息

测试前需要设置请求头：

```powershell
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }
```

请求示例：

```powershell
Invoke-RestMethod -Uri "http://121.40.241.161:8080/me" -Headers $headers
```

实际结果摘要：

```text
userId: user-admin
displayName: OpenTab 管理员
permissions: tab.approval.read, tab.calendar.read, tab.finance.read, ai.oncall
team.id: team-demo
team.name: 演示团队
```

结论：带 Bearer Token 后接口可用。未设置 `$headers` 时会返回 `UNAUTHORIZED`，这是符合预期的鉴权行为。

---

### 2.4 当前用户已启用 Tab

```powershell
Invoke-RestMethod -Uri "http://121.40.241.161:8080/tabs" -Headers $headers
```

管理员账号实际返回 Tab：

| id | entryType | route | 说明 |
|---|---|---|---|
| `approval` | `native` | `/approval` | 审批中心 |
| `calendar` | `native` | `/calendar` | 团队日程 |
| `finance` | `native` | `/finance` | 财务看板 |
| `next` | `native` | `/next` | 新版实验 Tab，`minContainerVersion=2` |
| `docs` | `web` | `/docs` | 接入文档 Web Tab |

结论：`GET /tabs` 可用，当前可支撑客户端动态 Tab 展示。

---

### 2.5 系统 Tab 目录

```powershell
Invoke-RestMethod -Uri "http://121.40.241.161:8080/tabs/catalog" -Headers $headers
```

实际结果：返回 `approval`、`calendar`、`finance`、`next`、`docs`。

结论：`GET /tabs/catalog` 可用，当前可用于客户端 Tab 管理页展示“可添加的内置 Tab”。

---

### 2.6 AI OnCall 旧版 SSE 流式接口

```powershell
curl.exe -N "http://121.40.241.161:8080/oncall/stream?message=%E5%A6%82%E4%BD%95%E6%8E%A5%E5%85%A5%E4%B8%80%E4%B8%AA%E4%B8%9A%E5%8A%A1Tab" -H "Authorization: Bearer $token" -H "Accept: text/event-stream"
```

实际结果摘要：

```text
event: delta
data: {"text":"..."}

event: delta
data: {"text":"..."}

event: done
data: {}
```

结论：旧版 AI OnCall SSE 接口可用。客户端今天可以先基于该接口实现 AI 聊天页面和流式回复展示。

---

### 2.7 停用当前用户 Tab

使用 `opentab-guest / guest123` 登录后，当前仅返回 `docs` Tab。

请求示例：

```powershell
Invoke-RestMethod -Uri "http://121.40.241.161:8080/me/tabs/docs" -Method Delete -Headers $headers
```

实际结果：

```text
success: true
tabId: docs
```

结论：`DELETE /me/tabs/{tabId}` 可用。客户端可以支持“停用我的 Tab”。

---

## 3. 已验证的权限/异常场景

### 3.1 未携带 Bearer Token

```powershell
Invoke-RestMethod -Uri "http://121.40.241.161:8080/me" -Headers $headers
```

如果 `$headers` 未正确设置，实际返回：

```json
{
  "code": "UNAUTHORIZED",
  "message": "缺少 Bearer Token"
}
```

结论：鉴权失败响应可用，客户端需要展示登录失效或未登录提示。

---

### 3.2 guest 启用无权限 Tab

使用 `opentab-guest / guest123` 登录后测试：

```powershell
$enableBody = @{ tabId = "approval" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://121.40.241.161:8080/me/tabs" -Method Post -Headers $headers -ContentType "application/json" -Body $enableBody
```

实际结果：

```json
{
  "code": "FORBIDDEN",
  "message": "当前账号无权启用审批中心"
}
```

结论：`POST /me/tabs` 接口存在，权限控制生效。客户端 Tab 管理页需要能展示“权限不足”。

---

### 3.3 guest 访问审批摘要

```powershell
Invoke-RestMethod -Uri "http://121.40.241.161:8080/business/approval/summary" -Headers $headers
```

实际结果：

```json
{
  "code": "FORBIDDEN",
  "message": "当前账号无权查看审批数据"
}
```

结论：审批接口权限控制生效。审批摘要接口本身还需要用 admin token 再补测一次。

---

## 4. 当前未实现的接口

### 4.1 创建用户自定义 Web Tab

```powershell
$customTab = @{
  id = "custom-docs-test"
  displayName = "测试网页"
  description = "客户端联调创建的自定义 Web Tab"
  icon = "docs"
  route = "/custom-docs-test"
  entryType = "web"
  entryUri = "https://example.com"
  minContainerVersion = 1
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://121.40.241.161:8080/tabs" -Method Post -Headers $headers -ContentType "application/json" -Body $customTab
```

实际结果：

```text
404 page not found
```

结论：`POST /tabs` 当前未实现。客户端可以先做自定义 Web Tab 的 UI、表单校验和 Repository 预留，但不能依赖真实创建成功。

---

### 4.2 注册接口

```powershell
$registerBody = @{
  account = "test-user-001"
  password = "test123456"
  displayName = "测试用户001"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://121.40.241.161:8080/auth/register" -Method Post -ContentType "application/json" -Body $registerBody
```

实际结果：

```text
404 page not found
```

结论：`POST /auth/register` 当前未实现。注册页可以先设计 UI，但真实注册逻辑需要等服务端确认接口。

---

### 4.3 新版 AI 会话创建接口

```powershell
$sessionBody = @{ title = "Tab 接入咨询" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://121.40.241.161:8080/oncall/sessions" -Method Post -Headers $headers -ContentType "application/json" -Body $sessionBody
```

实际结果：

```text
404 page not found
```

结论：`POST /oncall/sessions` 当前未实现。客户端 AI 聊天页先接旧版 `GET /oncall/stream?message=...`，后续再升级为 session/message 模型。

---

## 5. 待补测接口

以下接口尚未完成验证，建议服务端更新后继续测试。

### 5.1 已在文档中存在但本次未完整验证

| 接口 | 目的 | 当前状态 |
|---|---|---|
| `POST /me/tabs` | 启用当前用户已有权限的内置 Tab | 已验证权限不足场景，仍需用有权限且未启用的账号验证成功场景 |
| `GET /business/approval/summary` | 审批摘要 | guest 返回 FORBIDDEN，需用 admin token 验证成功场景 |
| `GET /business/calendar/summary` | 日程摘要 | 待测 |
| `GET /debug/status` | 调试状态 | 待测 |
| `POST /tabs/validate` | 校验 TabManifest | 待测 |
| `POST /tabs/{tabId}/actions/{actionId}` | 上报 Tab Action | 待测 |
| `GET /tabs/{tabId}` | 获取单个 Tab | 待测 |

### 5.2 当前规划中但云端未实现

| 接口 | 目的 | 当前状态 |
|---|---|---|
| `POST /auth/register` | 用户注册 | 404，未实现 |
| `POST /tabs` | 创建用户自定义 Web Tab | 404，未实现 |
| `PUT /tabs/{tabId}` | 修改用户自定义 Tab | 待服务端实现后测试 |
| `DELETE /tabs/{tabId}` | 删除用户自定义 Tab 配置 | 待服务端实现后测试 |
| `PUT /me/tabs/order` | 调整我的 Tab 顺序 | 待服务端实现后测试 |
| `POST /oncall/sessions` | 创建 AI 会话 | 404，未实现 |
| `GET /oncall/sessions` | 获取 AI 会话列表 | 待服务端实现后测试 |
| `POST /oncall/sessions/{sessionId}/messages` | 上传用户消息 | 待服务端实现后测试 |
| `GET /oncall/sessions/{sessionId}/messages` | 获取会话消息历史 | 待服务端实现后测试 |
| `GET /oncall/sessions/{sessionId}/stream?messageId=...` | 指定会话流式回复 | 待服务端实现后测试 |
| `DELETE /oncall/sessions/{sessionId}` | 删除 AI 会话 | 待服务端实现后测试 |

---

## 6. 对客户端开发的影响

### 6.1 可以直接接真实接口的功能

```text
登录
当前用户信息展示
动态 Tab 展示
系统 Tab 目录展示
停用我的 Tab
权限不足提示
AI OnCall 旧版 SSE 流式回复
```

### 6.2 需要先做 UI/状态和接口预留的功能

```text
注册账号
创建自定义 Web Tab
修改自定义 Tab
删除自定义 Tab 配置
AI 会话列表
AI 消息历史
指定会话的流式回复
```

### 6.3 今日客户端建议优先级

```text
1. AI OnCall 聊天页面：先接 GET /oncall/stream?message=...
2. Tab 管理页面：接 GET /tabs 和 GET /tabs/catalog
3. 内置 Tab 启用/停用：接 POST /me/tabs 和 DELETE /me/tabs/{tabId}，重点处理 FORBIDDEN
4. 自定义 Web Tab：先做表单、预览、校验、错误提示；真实提交等待 POST /tabs
5. 注册页：等 POST /auth/register 协议确认后再接真实提交
```

---

## 7. 需要和服务端继续确认的问题

1. `POST /auth/register` 是否纳入近期接口？成功响应是否复用登录响应结构。
2. `POST /tabs` 的 `id`、`route`、`version`、`sortOrder` 是客户端传入，还是服务端生成。
3. 自定义 Tab 第一版是否只支持 `entryType=web`。
4. `POST /me/tabs` 是否需要提供一个有权限但默认未启用某 Tab 的测试账号，用于验证成功启用场景。
5. AI OnCall 是否近期升级到 session/message 模型，还是第一版继续使用旧版 `GET /oncall/stream`。
6. SSE 事件是否固定为 `delta`、`tool`、`done`、`error`；`done` 是否返回最终 assistant messageId。
7. 是否需要为客户端提供稳定的错误码枚举，便于 UI 做中文错误提示和恢复操作。
