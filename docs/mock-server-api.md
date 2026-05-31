# Mock Server API

## 1. 通用请求格式

需要登录的接口使用 Bearer Token：

```http
Authorization: Bearer <token>
```

普通接口使用 JSON：

```http
Content-Type: application/json
Accept: application/json
```

AI OnCall 使用 SSE：

```http
Accept: text/event-stream
```

## 2. 通用错误格式

非 2xx 响应统一返回：

```json
{
  "code": "UNAUTHORIZED",
  "message": "Token 无效或已过期"
}
```

常见错误码：

| code | 说明 |
|---|---|
| INVALID_REQUEST | 请求格式错误 |
| INVALID_CREDENTIALS | 账号或密码不正确 |
| UNAUTHORIZED | 未登录或 token 无效 |
| FORBIDDEN | 当前账号无权限 |
| RESOURCE_NOT_FOUND | 资源不存在 |
| INVALID_TAB_CONFIG | Tab 配置不合法 |

## 3. 演示账号

| 账号 | 密码 | token | 已启用 Tab |
|---|---|---|---|
| opentab-demo | demo123 | mock-access-token | 审批中心、团队日程、新版实验 Tab |
| opentab-admin | admin123 | mock-admin-token | 审批中心、团队日程、财务看板、新版实验 Tab、接入文档 |
| opentab-guest | guest123 | mock-guest-token | 接入文档 |

## 4. TabManifest 格式

服务端下发的 Tab 配置统一使用 `TabManifest`。

```json
{
  "id": "approval",
  "displayName": "审批中心",
  "description": "处理待审批、已审批和发起审批。",
  "icon": "approval",
  "route": "/approval",
  "entryType": "native",
  "entryUri": "native://approval",
  "version": {
    "major": 1,
    "minor": 0,
    "patch": 0
  },
  "minContainerVersion": 1,
  "permissions": [
    "tab.approval.read"
  ],
  "enabled": true,
  "sortOrder": 10,
  "extension": {
    "titleBar": {
      "rightText": "刷新",
      "menuItems": [
        {
          "id": "filter",
          "label": "筛选"
        }
      ]
    },
    "fab": {
      "id": "create",
      "icon": "add",
      "label": "发起"
    }
  },
  "extraConfig": {
    "mockBusinessId": "approval-demo"
  }
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | Tab 唯一标识 |
| displayName | string | 展示名称 |
| description | string | 描述 |
| icon | string | 图标标识，客户端自行映射 |
| route | string | 客户端内部路由 |
| entryType | string | native、web、hybrid、external |
| entryUri | string | 入口地址 |
| version | object | Tab 配置版本 |
| minContainerVersion | int | 最低容器主版本 |
| permissions | string[] | 打开所需权限 |
| enabled | boolean | 当前用户是否启用 |
| sortOrder | int | 排序 |
| extension | object | JSON 安全的扩展配置 |
| extraConfig | object | 业务扩展配置 |

## 5. 健康检查

```http
GET /health
```

响应：

```json
{
  "mode": "mock",
  "serverTime": "2026-05-30T15:37:28+08:00",
  "service": "tab-container-server",
  "status": "ok"
}
```

## 6. 登录

```http
POST /auth/login
```

请求：

```json
{
  "account": "opentab-demo",
  "password": "demo123"
}
```

响应：

```json
{
  "token": "mock-access-token",
  "userId": "user-demo",
  "displayName": "OpenTab 演示账号",
  "permissions": [
    "tab.approval.read",
    "tab.calendar.read",
    "ai.oncall"
  ]
}
```

失败响应：

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "账号或密码不正确"
}
```

## 7. 退出登录

```http
POST /auth/logout
Authorization: Bearer <token>
```

响应：

```json
{
  "success": true
}
```

## 8. 当前用户

```http
GET /me
Authorization: Bearer <token>
```

响应：

```json
{
  "userId": "user-demo",
  "displayName": "OpenTab 演示账号",
  "permissions": [
    "tab.approval.read",
    "tab.calendar.read",
    "ai.oncall"
  ],
  "team": {
    "id": "team-demo",
    "name": "演示团队"
  }
}
```

## 9. 当前用户已启用 Tab

```http
GET /tabs
Authorization: Bearer <token>
```

响应：`TabManifest[]`

```json
[
  {
    "id": "approval",
    "displayName": "审批中心",
    "description": "处理待审批、已审批和发起审批。",
    "icon": "approval",
    "route": "/approval",
    "entryType": "native",
    "entryUri": "native://approval",
    "version": {
      "major": 1,
      "minor": 0,
      "patch": 0
    },
    "minContainerVersion": 1,
    "permissions": ["tab.approval.read"],
    "enabled": true,
    "sortOrder": 10
  }
]
```

## 10. 系统 Tab 目录

```http
GET /tabs/catalog
Authorization: Bearer <token>
```

响应：`TabManifest[]`

```json
[
  {
    "id": "finance",
    "displayName": "财务看板",
    "route": "/finance",
    "entryType": "native",
    "entryUri": "native://finance",
    "version": {
      "major": 1,
      "minor": 0,
      "patch": 0
    },
    "minContainerVersion": 1,
    "permissions": ["tab.finance.read"],
    "enabled": false,
    "sortOrder": 30
  }
]
```

## 11. 获取单个 Tab

```http
GET /tabs/{tabId}
Authorization: Bearer <token>
```

响应：单个 `TabManifest`

```json
{
  "id": "approval",
  "displayName": "审批中心",
  "route": "/approval",
  "entryType": "native",
  "entryUri": "native://approval",
  "version": {
    "major": 1,
    "minor": 0,
    "patch": 0
  },
  "minContainerVersion": 1,
  "permissions": ["tab.approval.read"],
  "enabled": true,
  "sortOrder": 10
}
```

未找到：

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Tab 不存在或当前账号未启用"
}
```

## 12. 启用 Tab

```http
POST /me/tabs
Authorization: Bearer <token>
```

请求：

```json
{
  "tabId": "docs"
}
```

响应：

```json
{
  "success": true,
  "tabId": "docs"
}
```

权限不足：

```json
{
  "code": "FORBIDDEN",
  "message": "当前账号无权启用财务看板"
}
```

## 13. 停用 Tab

```http
DELETE /me/tabs/{tabId}
Authorization: Bearer <token>
```

响应：

```json
{
  "success": true,
  "tabId": "docs"
}
```

## 14. 校验 Tab 配置

```http
POST /tabs/validate
Authorization: Bearer <token>
```

请求：

```json
{
  "containerVersion": 1,
  "permissions": [
    "tab.approval.read"
  ],
  "tab": {
    "id": "docs",
    "displayName": "接入文档",
    "route": "/docs",
    "entryType": "web",
    "entryUri": "https://example.com/docs",
    "version": {
      "major": 1,
      "minor": 0,
      "patch": 0
    },
    "minContainerVersion": 1,
    "permissions": [],
    "enabled": true
  }
}
```

响应：

```json
{
  "valid": true,
  "openable": true,
  "errors": [],
  "warnings": [],
  "normalizedTab": {
    "id": "docs",
    "displayName": "接入文档",
    "route": "/docs",
    "entryType": "web",
    "entryUri": "https://example.com/docs",
    "version": {
      "major": 1,
      "minor": 0,
      "patch": 0
    },
    "minContainerVersion": 1,
    "permissions": [],
    "enabled": true
  }
}
```

校验问题格式：

```json
{
  "code": "MISSING_REQUIRED_FIELD",
  "protocolCode": 1003,
  "message": "Tab 缺失必填字段：route",
  "field": "route"
}
```

## 15. 上报 Tab Action

```http
POST /tabs/{tabId}/actions/{actionId}
Authorization: Bearer <token>
```

请求：

```json
{
  "source": "titleBar",
  "payload": {
    "route": "/approval"
  }
}
```

响应：

```json
{
  "success": true,
  "message": "动作已触发",
  "next": {
    "type": "refresh",
    "text": "审批中心已刷新"
  }
}
```

`next.type` 可选：

| type | 说明 |
|---|---|
| toast | 客户端展示提示 |
| refresh | 客户端刷新当前 Tab |
| navigate | 客户端跳转 |
| none | 无后续动作 |

## 16. 审批摘要

```http
GET /business/approval/summary
Authorization: Bearer <token>
```

响应：

```json
{
  "pendingCount": 12,
  "approvedToday": 8,
  "items": [
    {
      "id": "apv-001",
      "title": "采购申请",
      "applicant": "张三",
      "status": "pending",
      "createdAt": "2026-05-30T10:00:00+08:00"
    }
  ]
}
```

## 17. 日程摘要

```http
GET /business/calendar/summary
Authorization: Bearer <token>
```

响应：

```json
{
  "todayCount": 3,
  "events": [
    {
      "id": "evt-001",
      "title": "项目周会",
      "startTime": "2026-05-30T14:00:00+08:00",
      "endTime": "2026-05-30T15:00:00+08:00",
      "location": "线上会议"
    }
  ]
}
```

## 18. AI OnCall SSE

```http
GET /oncall/stream?message=如何接入一个业务Tab
Authorization: Bearer <token>
Accept: text/event-stream
```

响应为 SSE 事件流：

```text
event: delta
data: {"text":"我会先根据 TabManifest 协议检查入口类型、权限和容器版本。"}

event: delta
data: {"text":"如果你贴出配置或日志，我可以继续返回诊断建议。"}

event: tool
data: {"name":"validate_tab_config","status":"完成","summary":"已检查协议字段、权限声明、版本约束和入口类型。"}

event: done
data: {}
```

事件说明：

| event | data 格式 | 说明 |
|---|---|---|
| delta | `{"text":"..."}` | AI 增量文本 |
| tool | `{"name":"...","status":"...","summary":"..."}` | 工具调用结果 |
| done | `{}` | 流结束 |
| error | `{"code":"...","message":"..."}` | 流式错误 |

## 19. Debug 状态

```http
GET /debug/status
Authorization: Bearer <token>
```

响应：

```json
{
  "apiVersion": "1.1-draft",
  "database": {
    "enabled": false,
    "type": "memory"
  },
  "mockMode": true,
  "serverTime": "2026-05-30T15:37:28+08:00",
  "sseAvailable": true,
  "tabCount": 5
}
```
