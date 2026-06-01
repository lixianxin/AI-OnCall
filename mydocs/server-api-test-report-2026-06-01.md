# 服务端接口测试报告（2026-06-01）

## 1. 测试结论

测试时间：2026-06-01 14:50 - 14:57（Asia/Shanghai）

测试目标：

```text
http://121.40.241.161:8080
```

测试依据：

```text
docs/mock-server-api.md
docs/protocol-v1.0.md
docs/待补接口规划.md
```

本轮结论：

1. 服务端基础连通正常，`GET /health` 返回 200。
2. 多个交接文档中标记为“待补”的接口已经实现，包括：
   - `POST /auth/register`
   - `POST /tabs`
   - `PUT /tabs/{tabId}`
   - `DELETE /tabs/{tabId}`
   - `GET /debug/permissions`
   - `GET /debug/sample-tabs`
   - AI OnCall session/message 系列接口
   - 审批列表、审批详情
   - 日程列表、日程详情
3. 但 `GET /debug/status` 当前仍返回：

```json
{
  "database": {
    "enabled": false,
    "type": "memory"
  },
  "mockMode": true
}
```

这与“服务端已经连数据库”的说法不一致。可能原因是当前公网地址仍指向 mock/memory 环境，或 debug 状态字段未同步更新。建议服务端同学确认部署环境和 `/debug/status` 的实现。

4. `GET /me` 当前没有返回 `avatarUrl` 字段，客户端头像仍需保留本地默认/本地选择兜底。
5. `POST /auth/logout` 在无 token 时也返回 200 `{"success":true}`，如果协议要求登录态接口统一鉴权，这里需要服务端确认。
6. `GET /tabs/next` 是新版实验 Tab 的实际 id；`GET /tabs/new-exp` 返回 404。客户端/文档中如有 `new-exp` 表述应改为 `next`。

## 2. 测试方法

使用 Python 标准库 `urllib.request` 发起 HTTP 请求，记录：

```text
HTTP 方法
接口路径
请求 token 类型
HTTP 状态码
Content-Type
响应体关键字段
耗时
```

写接口分为两类：

1. 可恢复写入：执行完整创建/查询/更新/删除或启用/停用/恢复闭环。
2. 不易恢复写入：只测非法参数或不存在资源，避免污染业务数据。

本轮已清理的临时数据：

```text
codex-api-test-20260601065345   已删除
codex-dup-20260601065508442     已删除
codex-cn-20260601065614156      已删除
sess-1780296838002525141        已删除
sess-1780296980428690615        已删除
```

本轮有一个注册成功测试账号无法通过公开接口删除：

```text
account: codex-test-20260601065508442
userId:  user-2b9be346f357008e
```

如果当前环境已接真实数据库，建议服务端后续清理该测试账号，或提供测试用户删除接口。

## 3. 账号与鉴权

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 健康检查 | GET | `/health` | 200 | 服务可达，返回 `mode=mock` |
| Debug 状态（无 token） | GET | `/debug/status` | 401 | 返回 `UNAUTHORIZED` |
| Debug 状态（demo/admin） | GET | `/debug/status` | 200 | 返回 `mockMode=true`、`database.enabled=false` |
| 登录 demo | POST | `/auth/login` | 200 | 返回 `mock-access-token` |
| 登录 admin | POST | `/auth/login` | 200 | 返回 `mock-admin-token` |
| 登录 guest | POST | `/auth/login` | 200 | 返回 `mock-guest-token` |
| 错误密码 | POST | `/auth/login` | 401 | `INVALID_CREDENTIALS` |
| `/me` 无 token | GET | `/me` | 401 | `缺少 Bearer Token` |
| `/me` 错 token | GET | `/me` | 401 | `Token 无效或已过期` |
| 退出登录（有 token） | POST | `/auth/logout` | 200 | `success=true` |
| 退出登录（无 token） | POST | `/auth/logout` | 200 | 与鉴权约定不完全一致，建议确认 |

`GET /me` 返回字段：

```json
{
  "userId": "user-demo",
  "displayName": "OpenTab 演示账号",
  "permissions": [
    "ai.oncall",
    "tab.approval.read",
    "tab.calendar.read"
  ],
  "team": {
    "id": "team-demo",
    "name": "演示团队"
  }
}
```

缺失字段：

```text
avatarUrl
```

## 4. 注册接口

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 空请求体 | POST | `/auth/register` | 400 | `账号和密码不可为空` |
| 密码过短 | POST | `/auth/register` | 400 | `密码长度不能少于 6` |
| 正常注册 | POST | `/auth/register` | 200 | 返回 token/userId/displayName/permissions |
| 重复注册 | POST | `/auth/register` | 409 | `ACCOUNT_EXISTS` |
| 新账号登录 | POST | `/auth/login` | 200 | 注册后的账号可登录 |

结论：注册接口已实现，客户端可以从“接口暂未开放”升级为真实注册流程。

注意：当前公开协议里没有删除测试用户接口，本轮注册账号无法自助清理。

## 5. Tab 读取接口

| 用例 | 方法 | 路径 | demo | admin | guest | 说明 |
|---|---:|---|---:|---:|---:|---|
| 我的 Tab | GET | `/tabs` | 200 | 200 | 200 | 按账号权限返回已启用 Tab |
| Tab 目录 | GET | `/tabs/catalog` | 200 | 200 | 200 | 返回 catalog |
| 审批中心 | GET | `/tabs/approval` | 200 | 200 | - | 正常 |
| 团队日程 | GET | `/tabs/calendar` | 200 | 200 | - | 正常 |
| 财务看板 | GET | `/tabs/finance` | - | 200 | - | admin 可见 |
| 接入文档 | GET | `/tabs/docs` | 启用后 200 | 200 | - | web Tab |
| 新版实验 Tab | GET | `/tabs/next` | 200 | 200 | 404 | 实际 id 是 `next` |
| 不存在 Tab | GET | `/tabs/missing-tab` | - | 404 | - | `RESOURCE_NOT_FOUND` |

admin `/tabs` 当前返回 5 个：

```text
approval
calendar
finance
next
docs
```

demo `/tabs` 当前返回 3 个：

```text
approval
calendar
next
```

guest `/tabs` 当前仅有可访问范围内的 Tab。

TabManifest 关键字段整体符合协议：

```text
id / displayName / description / icon / route / entryType / entryUri
version / minContainerVersion / permissions / enabled / sortOrder
extension / extraConfig
```

## 6. Tab 启用/停用

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| demo 启用 docs | POST | `/me/tabs` | 200 | `{"tabId":"docs","success":true}` |
| demo 查询已启用 | GET | `/tabs` | 200 | docs 出现在列表中 |
| demo 停用 docs | DELETE | `/me/tabs/docs` | 200 | 已恢复测试前状态 |
| demo 启用 finance | POST | `/me/tabs` | 403 | `FORBIDDEN` |
| guest 启用 finance | POST | `/me/tabs` | 403 | `FORBIDDEN` |
| admin 启用不存在 Tab | POST | `/me/tabs` | 404 | `RESOURCE_NOT_FOUND` |

结论：启用/停用主链路可用，权限控制符合预期。

## 7. 自定义 Tab 管理

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 创建 Web Tab | POST | `/tabs` | 200 | 创建成功并默认启用 |
| 查询新 Tab | GET | `/tabs/{tabId}` | 200 | 可查到新建配置 |
| 修改 Web Tab | PUT | `/tabs/{tabId}` | 200 | displayName、entryUri、sortOrder 可更新 |
| 删除 Web Tab | DELETE | `/tabs/{tabId}` | 200 | 删除成功 |
| 删除后查询 | GET | `/tabs/{tabId}` | 404 | 已清理 |
| 非法 route/entryUri | POST | `/tabs` | 400 | `INVALID_TAB_CONFIG` |
| 重复 route | POST | `/tabs` | 409 | `RESOURCE_CONFLICT` |
| 中文字段 | POST/GET/DELETE | `/tabs` | 200 | UTF-8 正常 |

创建响应示例：

```json
{
  "success": true,
  "tabId": "codex-cn-20260601065614156",
  "tab": {
    "id": "codex-cn-20260601065614156",
    "displayName": "中文Tab测试",
    "description": "中文描述校验",
    "icon": "docs",
    "route": "/codex-cn-20260601065614156",
    "entryType": "web",
    "entryUri": "https://example.com/cn",
    "version": {
      "major": 1,
      "minor": 0
    },
    "minContainerVersion": 1,
    "permissions": [],
    "enabled": true
  }
}
```

结论：自定义 Web Tab 创建/修改/删除闭环已可用，客户端可以接入真实接口。

## 8. Tab 校验与 Action

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 合法 Tab 校验 | POST | `/tabs/validate` | 200 | `valid=true`、`openable=true` |
| 缺字段校验 | POST | `/tabs/validate` | 200 | `valid=false`，errors 含 `protocolCode=1003` |
| 审批 Action | POST | `/tabs/approval/actions/filter` | 200 | 返回 `next.type=toast` |
| 不存在 Tab Action | POST | `/tabs/missing-tab/actions/filter` | 404 | `RESOURCE_NOT_FOUND` |

说明：`/tabs/validate` 对校验失败仍返回 HTTP 200，并在业务字段里返回 `valid=false`，客户端需要按业务字段判断。

## 9. Debug 接口

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| Debug 状态 | GET | `/debug/status` | 200 | 已实现 |
| 权限列表 | GET | `/debug/permissions` | 200 | 已实现，返回 4 个权限 |
| 示例 TabManifest | GET | `/debug/sample-tabs` | 200 | 已实现，返回 5 个示例 Tab |

`/debug/status` 关键响应：

```json
{
  "apiVersion": "1.1-draft",
  "database": {
    "enabled": false,
    "type": "memory"
  },
  "mockMode": true,
  "sseAvailable": true,
  "tabCount": 5
}
```

这是本轮最需要服务端确认的状态差异。

## 10. 业务接口

### 10.1 审批

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 审批摘要 demo/admin | GET | `/business/approval/summary` | 200 | 返回 pendingCount、approvedToday、items |
| 审批摘要 guest | GET | `/business/approval/summary` | 403 | 权限不足 |
| 审批列表 | GET | `/business/approval/items?status=all` | 200 | 已实现 |
| 待审批列表 | GET | `/business/approval/items?status=pending` | 200 | 已实现 |
| 审批详情 | GET | `/business/approval/items/user-admin-apv-001` | 200 | 已实现 |
| 不存在审批详情 | GET | `/business/approval/items/missing-item` | 404 | `审批记录不存在` |
| 不存在审批通过 | POST | `/business/approval/items/missing-item/approve` | 404 | 错误格式正常 |
| 不存在审批驳回 | POST | `/business/approval/items/missing-item/reject` | 404 | 错误格式正常 |

未执行成功审批通过/驳回：因为会改变种子数据状态，且没有恢复接口。建议服务端提供专用测试审批记录或重置接口后再测成功路径。

### 10.2 日程

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 日程摘要 demo/admin | GET | `/business/calendar/summary` | 200 | 2026-06-01 当天返回空列表 |
| 日程摘要 guest | GET | `/business/calendar/summary` | 403 | 权限不足 |
| 日程列表 2026-05-31 | GET | `/business/calendar/events?date=2026-05-31` | 200 | 返回 2 条 |
| 日程列表 2026-06-01 | GET | `/business/calendar/events?date=2026-06-01` | 200 | 返回空数组 |
| 日程详情 | GET | `/business/calendar/events/user-admin-evt-001` | 200 | 已实现 |
| 不存在日程详情 | GET | `/business/calendar/events/missing-event` | 404 | `日程不存在` |
| 新增日程非法参数 | POST | `/business/calendar/events` | 400 | `title、startTime、endTime 不可为空` |

未执行成功新增日程：因为当前公开协议没有删除日程接口，避免留下测试数据。

## 11. AI OnCall 接口

### 11.1 旧版单轮 SSE

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 单轮流式问答 | GET | `/oncall/stream?message=...` | 200 | `Content-Type: text/event-stream` |

事件流示例：

```text
event: delta
data: {"text":"我会先根据 TabManifest 协议检查入口类型、权限和容器版本。"}

event: delta
data: {"text":"如果你贴出配置或日志，我可以继续返回诊断建议。"}

event: done
data: {}
```

### 11.2 新版会话接口

| 用例 | 方法 | 路径 | 结果 | 说明 |
|---|---:|---|---:|---|
| 会话列表 | GET | `/oncall/sessions` | 200 | 已实现 |
| 创建会话 | POST | `/oncall/sessions` | 200 | 返回 sessionId |
| 上传用户消息 | POST | `/oncall/sessions/{sessionId}/messages` | 200 | 返回 messageId |
| 获取消息记录 | GET | `/oncall/sessions/{sessionId}/messages` | 200 | 返回消息列表 |
| 指定会话流式响应 | GET | `/oncall/sessions/{sessionId}/stream?messageId=...` | 200 | SSE 正常，done 返回 assistant messageId |
| 删除会话 | DELETE | `/oncall/sessions/{sessionId}` | 200 | 已实现 |
| 删除后查消息 | GET | `/oncall/sessions/{sessionId}/messages` | 404 | `AI 会话不存在` |
| 中文标题/消息 | POST/GET/DELETE | `/oncall/sessions` | 200 | UTF-8 正常 |

指定会话 SSE 示例：

```text
event: delta
data: {"text":"我会先根据 TabManifest 协议检查入口类型、权限和容器版本。"}

event: delta
data: {"text":"如果你贴出配置或日志，我可以继续返回诊断建议。"}

event: done
data: {"messageId":"msg-1780296842882544300"}
```

结论：新版 AI session/message 接口已经可用，客户端可以从旧 `/oncall/stream` 升级为多轮会话模型。

## 12. 协议差异与问题清单

### P0：需要服务端确认

1. 数据库状态不一致：
   - 服务端同学说已接数据库。
   - 实测 `/debug/status` 返回 `database.enabled=false`、`type=memory`、`mockMode=true`。
   - 建议确认公网地址是否已切到数据库版本，或修正 debug 状态。

2. `POST /auth/logout` 无 token 仍返回 200：
   - 如果 logout 设计为幂等接口，可以接受。
   - 如果要求登录接口统一鉴权，应改为 401。

3. 注册用户没有删除接口：
   - 测试和演示环境会产生不可清理账号。
   - 建议提供测试环境清理能力或 debug reset 接口。

### P1：客户端需要尽快适配

1. 注册接口已可用：客户端可以移除“注册接口暂未开放”的固定提示，接真实注册成功路径。
2. 自定义 Web Tab 创建/修改/删除已可用：客户端可以完成自定义 Tab 闭环。
3. AI session/message 接口已可用：客户端可以升级为多轮会话。
4. Debug 权限和示例 Tab 接口已可用：可用于调试页或 AI 工具能力。

### P2：文档建议更新

1. `docs/待补接口规划.md` 中部分接口已经实现，应从“待补”移到正式接口文档。
2. `GET /tabs/next` 是实验 Tab 实际 id，建议统一文档表述。
3. `/me` 当前仍缺 `avatarUrl`，若短期不做，应在协议里标注为待补字段。

## 13. 客户端后续建议

建议客户端按这个顺序接入：

1. 保持当前登录逻辑，新增注册成功后的真实登录态处理。
2. 自定义 Web Tab：接入 `POST /tabs`、`PUT /tabs/{tabId}`、`DELETE /tabs/{tabId}`，完成新增/编辑/删除闭环。
3. AI oncall：从旧单轮 SSE 升级为 session/message 模型。
4. “我的”页继续等待 `/me.avatarUrl`，当前本地头像兜底保留。
5. 对 `/debug/status` 的数据库状态先不展示给普通用户，避免和服务端部署状态不一致造成困惑。

