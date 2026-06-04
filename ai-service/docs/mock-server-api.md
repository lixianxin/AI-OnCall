# AI OnCall 知识库 — 完整参考

> **版本**：v1.0 | **最后更新**：2026-06-02 | **本文件为 RAG 核心知识库，AI OnCall 严格依据此文档回答问题**

---

## 目录

1. [项目概述](#1-项目概述)
2. [系统架构](#2-系统架构)
3. [AI Service 详解](#3-ai-service-详解)
4. [SSE 事件协议参考](#4-sse-事件协议参考)
5. [RAG 检索引擎](#5-rag-检索引擎)
6. [Container Server API](#6-container-server-api)
7. [TabManifest 参考](#7-tabmanifest-参考)
8. [业务 Tab 接入协议](#8-业务-tab-接入协议)
9. [Android 客户端集成](#9-android-客户端集成)
10. [部署运维](#10-部署运维)
11. [常见问题排查](#11-常见问题排查)
12. [扩展指南](#12-扩展指南)

---

## 1. 项目概述

AI OnCall 是 OpenTab 容器中的 **AI 智能助手模块**，基于 RAG（检索增强生成）架构实现文档问答。

| 维度 | 技术选型 |
|------|----------|
| 服务端框架 | Spring Boot 3.4.5 + WebFlux（Netty） |
| AI 模型 | DeepSeek Chat（`deepseek-chat`，流式输出） |
| 检索引擎 | 自研 BM25 + 双字 Bigram 中文分词 + 标题层级分段 |
| RAG 知识库 | docs/ 目录下所有 .md 文件，启动时加载 |
| 客户端 | Android Kotlin + Jetpack Compose + OkHttp |
| 通信协议 | HTTP POST + SSE（Server-Sent Events） |
| Java 版本 | JDK 17 |

核心能力：
- 用户自然语言提问 → 自动检索知识库 → DeepSeek 生成回答 → SSE 流式输出
- 支持多轮对话上下文记忆（最近 10 轮）
- 意图识别（协议问答 / 错误诊断 / 代码生成 / 通用对话）
- Android 端流式渲染、工具卡片展示、来源文档展示、失败重试

---

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Android 客户端                             │
│                                                             │
│  OpenOnCallPage (Compose UI)                                 │
│    ↓ POST /api/chat/stream + SSE                             │
│  OpenOnCallRepository → OnCallApiClient                      │
│    ↓ JSON body {message, conversationId}                     │
└────────────────────────┬────────────────────────────────────┘
                         │  HTTP POST（端口 8081）
                         │  Accept: text/event-stream
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                    ai-service (Spring Boot WebFlux)           │
│                                                             │
│  ChatController — POST /api/chat/stream                      │
│    ↓                                                        │
│  DocumentAgentOrchestrator (RAG 编排)                        │
│    ├─ Intent 识别 → detectIntent(message)                    │
│    ├─ 文档检索 → DocumentStore.search(query, topK=5)         │
│    ├─ 上下文构建 → DeepSeekClient.stream(prompt)             │
│    └─ 事件流 → Intent → Sources → Tool → Content → Done      │
│                                                             │
│  DocumentStore:                                              │
│    ├─ 从 docs/ 加载 .md 文件                                  │
│    ├─ 按 Markdown 标题分块（## / ### 层级）                    │
│    ├─ 每个块前缀标题路径（如「AI Service > 配置项」）            │
│    └─ BM25 索引 + 标题匹配加分                                │
└─────────────────────────────────────────────────────────────┘
```

### 服务划分

项目包含两个独立的后端服务：

| 服务 | 端口 | 地址 | 技术栈 | 职责 |
|------|------|------|--------|------|
| **ai-service** | 8081 | `121.40.241.161:8081` | Spring Boot WebFlux | AI 聊天、RAG 检索、SSE 流式输出 |
| **container-server** (Mock) | 8080 | `121.40.241.161:8080` | Mock 服务 | 登录鉴权、Tab CRUD、业务数据 |

两个服务独立部署，Android 客户端通过不同的 API Client 分别调用。

---

## 3. AI Service 详解

### 3.1 流式聊天接口

```
POST /api/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

请求体：

```json
{
  "message": "如何注册Tab",
  "conversationId": "android-1748860800000"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 是 | 用户输入的问题 |
| conversationId | string | 否 | 会话 ID，不传则服务端自动生成 UUID |

当前接口不携带鉴权头。后续如需 API Key 或独立 Token，在 `OnCallApiClient` 构造函数注入 Header 即可。

### 3.2 配置文件

`application.yml`：

```yaml
server:
  port: 8081

deepseek:
  api-key: sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  base-url: https://api.deepseek.com
  model: deepseek-chat

oncall:
  docs-path: ${ONCALL_DOCS_PATH:../docs}
  # 远端知识库地址（可选），配置后启动时自动下载覆盖本地文件
  docs-remote-url: ${ONCALL_DOCS_REMOTE_URL:}

logging:
  level:
    com.oncall.ai: DEBUG
```

### 3.3 意图识别规则

`DocumentAgentOrchestrator.detectIntent()` 通过关键词匹配判断意图：

| 意图 | 常量 | 触发关键词 |
|------|------|-----------|
| 协议问答 | `PROTOCOL_QA` | tab、协议、注册、权限、路由、配置、接入、manifest |
| 错误诊断 | `ERROR_DIAGNOSIS` | exception、error、at、caused by、nullpointer、stacktrace、错误、异常、失败、报错、日志、log |
| 代码生成 | `CODE_GENERATION` | 代码、生成、写一个、示例、example、compose、kotlin |
| 通用对话 | `GENERAL_CHAT` | 以上均不匹配时 |

### 3.4 工具类型

`DocumentAgentOrchestrator.detectTool()` 根据意图返回对应工具：

| 工具 | 常量 | 对应意图 | 说明 |
|------|------|---------|------|
| search | `ToolEvent.SEARCH` | PROTOCOL_QA / GENERAL_CHAT | 搜索知识库 |
| analyze_log | `ToolEvent.ANALYZE_LOG` | ERROR_DIAGNOSIS | 分析日志 |
| generate | `ToolEvent.GENERATE` | CODE_GENERATION | 代码生成 |

### 3.5 多轮对话

- `ConversationStore` 使用 `ConcurrentHashMap` 内存存储
- 每次请求保留最近 10 条消息作为上下文
- 消息角色：USER / ASSISTANT / SYSTEM
- 仅保存纯文本（TEXT 类型），工具调用记录暂不持久化

### 3.6 会话管理

```kotlin
// Android 端生成会话 ID
val body = JSONObject().apply {
    put("message", content)
    put("conversationId", "android-" + System.currentTimeMillis())
}
```

- 服务端根据 `conversationId` 找回历史消息
- 不传 `conversationId` 则自动生成新会话
- 当前为内存存储，重启后会话丢失

---

## 4. SSE 事件协议参考

### 4.1 协议概览

所有事件统一使用 `event: message` 发送，事件类型由 JSON 中的 `type` 字段区分。

发送顺序（固定）：`sources → intent → tool → content × N → done`

### 4.2 事件类型速查

| type | 出现次数 | JSON 字段 | 说明 |
|------|----------|-----------|------|
| sources | 0~1 次 | `sources: string[]` | 知识来源文档列表 |
| intent | 1 次 | `intent: string` | 意图识别结果 |
| tool | 1 次 | `tool, status, summary` | 工具调用 |
| content | 0~N 次 | `delta: string` | AI 增量文本 |
| done | 1 次 | `messageId: string` | 流结束 |
| error | 0~1 次 | `code, delta` | 错误信息 |

### 4.3 各事件详细格式

#### sources 事件

告知客户端本次回答引用了哪些文档，UI 展示"来源文档"卡片。

```json
{
  "type": "sources",
  "sources": ["protocol-v1.0.md", "mock-server-api.md"]
}
```

#### intent 事件

```json
{
  "type": "intent",
  "intent": "PROTOCOL_QA"
}
```

Android 端映射为中文标签：

| 服务端值 | 客户端显示 |
|----------|-----------|
| PROTOCOL_QA | 协议问答 |
| ERROR_DIAGNOSIS | 错误诊断 |
| CODE_GENERATION | 代码生成 |
| GENERAL_CHAT | 通用助手 |

#### tool 事件

```json
{
  "type": "tool",
  "tool": "search",
  "status": "done",
  "summary": "已检索知识库，找到 5 条相关文档片段"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| tool | string | 工具名称：search / analyze_log / generate |
| status | string | 状态：running / done |
| summary | string | 中文描述摘要 |

#### content 事件

```json
{
  "type": "content",
  "delta": "根据协议规定，注册Tab需要完成以下三步..."
}
```

#### done 事件

```json
{
  "type": "done",
  "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### error 事件

```json
{
  "type": "error",
  "code": "STREAM_ERROR",
  "delta": "DeepSeek API returned 401: invalid api key"
}
```

常见错误码：

| 错误码 | 含义 |
|--------|------|
| STREAM_ERROR | DeepSeek API 调用失败 |
| SERIALIZE_ERROR | 事件 JSON 序列化失败 |
| NETWORK_ERROR | Android 端网络异常 |
| SSE_PARSE_ERROR | Android 端 SSE 解析失败 |

### 4.4 SSE 原始输出示例

```
event: message
data: {"type":"sources","sources":["protocol-v1.0.md"]}

event: message
data: {"type":"intent","intent":"PROTOCOL_QA"}

event: message
data: {"type":"tool","tool":"search","status":"done","summary":"已检索知识库，找到 3 条相关文档片段"}

event: message
data: {"type":"content","delta":"注册Tab需要完成以下三步：\n\n1. 实现 TabLifecycle 接口..."}

event: message
data: {"type":"done","messageId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890"}
```

### 4.5 Service 端实现要点

- `ChatController` 使用 `@PostMapping(produces = TEXT_EVENT_STREAM_VALUE)`
- `ChatEvent` 是 sealed interface，Jackson `@JsonTypeInfo` 自动注入 `type` 字段
- 6 种实现：`SourcesEvent` / `IntentEvent` / `ToolEvent` / `ContentEvent` / `DoneEvent` / `ErrorEvent`
- 发生错误时流不会中断，而是发送 `ErrorEvent` 后正常 complete

### 4.6 Android 端解析

```kotlin
// OpenOnCallRepository.parseSseLine()
when (obj.optString("type")) {
    "sources" -> OnCallStreamEvent.Sources(sources = list)
    "intent" -> OnCallStreamEvent.Intent(intent = ...)
    "tool" -> OnCallStreamEvent.Tool(tool = OnCallToolEvent(...))
    "content" -> OnCallStreamEvent.Delta(text = ...)
    "done" -> OnCallStreamEvent.Done(messageId = null)
    "error" -> OnCallStreamEvent.Error(code = ..., message = ...)
    else -> OnCallStreamEvent.Unknown(...)
}
```

---

## 5. RAG 检索引擎

### 5.1 文档加载流程

`DocumentStore` 在 `@PostConstruct` 阶段执行：

```
启动
  │
  ├─ (可选) 若 ONCALL_DOCS_REMOTE_URL 已配置，下载 .md 覆盖本地目录
  │   ├─ 单文件模式：URL 以 .md 结尾，直接下载
  │   └─ Manifest 模式：URL 返回文本，每行一个 .md URL
  │
  ├─ 扫描 docs/ 目录下所有 .md 文件（递归）
  │
  ├─ 按 Markdown 标题分块
  │   ├─ # → H1 标题层级
  │   ├─ ## → H2 标题层级（如「AI Service > 配置项」）
  │   └─ ### → H3 标题层级
  │
  ├─ 每个 Chunk 前缀标题路径 breadcrumb
  │   如：「TabManifest 参考 > 字段说明」
  │
  ├─ 长章节按 600 字切分（80 字重叠）
  │
  └─ 构建 BM25 倒排索引
```

### 5.2 分词策略

```java
// 中文：连续 CJK 字符 → 双字 Bigram
"如何注册Tab" → ["如何", "何注", "注册"]

// 英文：按空白分割，保留 >= 2 字符的词
"TabManifest required fields" → ["tabmanifest", "required", "fields"]
```

### 5.3 检索与排序

检索流程：

1. 对用户查询做同样分词
2. BM25 计算每个 chunk 的 TF-IDF 分数
3. 标题匹配加分：如果 chunk 的标题路径包含查询词，加 30% 奖励分
4. 取 Top-5（仅返回分数 > 0 的）

```java
// score = BM25(query, chunk) + titleMatchBonus
// titleMatchBonus: 每匹配一个中文单字 +0.03，每匹配一个英文词 +0.3，上限 1.5
```

### 5.4 Prompt 构造

检索到的 Top-5 Chunk 被注入 LLM Prompt：

```
Messages 数组：
1. system: "你是 OpenTab 协议专家...（回答规则）"
2. system: "以下是知识库相关文档：\n[文件名1] 标题路径 > ...内容...\n\n---\n\n[文件名2]..."
3. user: "...（用户原始问题）"
```

### 5.5 System Prompt（回答约束）

回答规则：
1. **只能根据知识库文档回答，禁止使用 LLM 内置知识**
2. 知识库存在答案 → 引用协议名称和关键字段，给出结构化说明，附示例代码
3. 知识库不存在答案 → 明确回答"当前知识库中未找到相关信息"，给出研究方向
4. 回答风格：中文，结构清晰，技术术语保持英文原名
5. 引用时标注来源文档名称

### 5.6 添加新知识文档

1. 将 `.md` 文件放入 `docs/` 目录
2. 给文件起能体现内容的文件名（如 `rag-internals.md`）
3. 使用 `#`、`##`、`###` 标题组织结构
4. 重启服务 → `DocumentStore` 自动加载索引

最佳实践：
- 每个 `##` 小节聚焦一个可回答的问题
- 表格、列表等结构化内容对检索更友好
- 文件名应包含关键词（如 `deployment-guide.md`）

---

## 6. Container Server API

> Container Server（Mock）部署地址：`http://121.40.241.161:8080`。提供登录鉴权、Tab 配置管理、业务数据等能力。

### 6.1 通用请求格式

```http
Authorization: Bearer <token>
Content-Type: application/json
```

### 6.2 通用错误格式

```json
{
  "code": "UNAUTHORIZED",
  "message": "Token 无效或已过期"
}
```

| 错误码 | 说明 |
|--------|------|
| INVALID_REQUEST | 请求格式错误 |
| INVALID_CREDENTIALS | 账号或密码不正确 |
| UNAUTHORIZED | 未登录或 token 无效 |
| FORBIDDEN | 当前账号无权限 |
| RESOURCE_NOT_FOUND | 资源不存在 |
| INVALID_TAB_CONFIG | Tab 配置不合法 |

### 6.3 演示账号

| 账号 | 密码 | token | 已启用 Tab |
|------|------|-------|-----------|
| opentab-demo | demo123 | mock-access-token | 审批中心、团队日程、新版实验 Tab |
| opentab-admin | admin123 | mock-admin-token | 审批中心、团队日程、财务看板、新版实验 Tab、接入文档 |
| opentab-guest | guest123 | mock-guest-token | 接入文档 |

### 6.4 健康检查

```http
GET /health
```

```json
{
  "mode": "mock",
  "serverTime": "2026-05-30T15:37:28+08:00",
  "service": "tab-container-server",
  "status": "ok"
}
```

### 6.5 登录

```http
POST /auth/login
```

```json
{ "account": "opentab-demo", "password": "demo123" }
```

成功：

```json
{
  "token": "mock-access-token",
  "userId": "user-demo",
  "displayName": "OpenTab 演示账号",
  "permissions": ["tab.approval.read", "tab.calendar.read", "ai.oncall"]
}
```

失败：

```json
{ "code": "INVALID_CREDENTIALS", "message": "账号或密码不正确" }
```

### 6.6 当前用户

```http
GET /me
Authorization: Bearer <token>
```

### 6.7 Tab 管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/tabs` | GET | 当前用户已启用 Tab 列表 |
| `/tabs/catalog` | GET | 系统全部 Tab 目录 |
| `/tabs/{tabId}` | GET | 获取单个 Tab 配置 |
| `/me/tabs` | POST | 启用 Tab（body: `{tabId}`）|
| `/me/tabs/{tabId}` | DELETE | 停用 Tab |
| `/tabs/validate` | POST | 校验 Tab 配置合法性 |

### 6.8 校验 Tab 配置

```http
POST /tabs/validate
Authorization: Bearer <token>
```

```json
{
  "containerVersion": 1,
  "permissions": ["tab.approval.read"],
  "tab": {
    "id": "docs",
    "displayName": "接入文档",
    "route": "/docs",
    "entryType": "web",
    "entryUri": "https://example.com/docs",
    "version": { "major": 1, "minor": 0, "patch": 0 },
    "minContainerVersion": 1,
    "permissions": [],
    "enabled": true
  }
}
```

校验结果格式：

```json
{
  "valid": true,
  "openable": true,
  "errors": [],
  "warnings": [],
  "normalizedTab": { ... }
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

### 6.9 上报 Tab Action

```http
POST /tabs/{tabId}/actions/{actionId}
Authorization: Bearer <token>
```

```json
{ "source": "titleBar", "payload": { "route": "/approval" } }
```

响应中 `next.type` 可选值：

| type | 说明 |
|------|------|
| toast | 客户端展示提示 |
| refresh | 客户端刷新当前 Tab |
| navigate | 客户端跳转 |
| none | 无后续动作 |

### 6.10 业务数据

#### 审批摘要

```http
GET /business/approval/summary
Authorization: Bearer <token>
```

```json
{
  "pendingCount": 12,
  "approvedToday": 8,
  "items": [
    { "id": "apv-001", "title": "采购申请", "applicant": "张三", "status": "pending", "createdAt": "2026-05-30T10:00:00+08:00" }
  ]
}
```

#### 日程摘要

```http
GET /business/calendar/summary
Authorization: Bearer <token>
```

```json
{
  "todayCount": 3,
  "events": [
    { "id": "evt-001", "title": "项目周会", "startTime": "2026-05-30T14:00:00+08:00", "endTime": "2026-05-30T15:00:00+08:00", "location": "线上会议" }
  ]
}
```

### 6.11 Debug 状态

```http
GET /debug/status
Authorization: Bearer <token>
```

```json
{
  "apiVersion": "1.1-draft",
  "database": { "enabled": false, "type": "memory" },
  "mockMode": true,
  "serverTime": "2026-05-30T15:37:28+08:00",
  "sseAvailable": true,
  "tabCount": 5
}
```

---

## 7. TabManifest 参考

### 7.1 TabManifest 完整格式

服务端下发的 Tab 配置统一使用 `TabManifest`：

```json
{
  "id": "approval",
  "displayName": "审批中心",
  "description": "处理待审批、已审批和发起审批。",
  "icon": "approval",
  "route": "/approval",
  "entryType": "native",
  "entryUri": "native://approval",
  "version": { "major": 1, "minor": 0, "patch": 0 },
  "minContainerVersion": 1,
  "permissions": ["tab.approval.read"],
  "enabled": true,
  "sortOrder": 10,
  "extension": {
    "titleBar": { "rightText": "刷新", "menuItems": [{"id": "filter", "label": "筛选"}] },
    "fab": { "id": "create", "icon": "add", "label": "发起" }
  },
  "extraConfig": { "mockBusinessId": "approval-demo" }
}
```

### 7.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | Tab 唯一标识 |
| displayName | string | 是 | 展示名称，≤ 16 字符 |
| description | string | 否 | 描述 |
| icon | string | 否 | 图标标识，客户端自行映射 |
| route | string | 是 | 客户端内部路由，`/` 开头 |
| entryType | string | 是 | `native` / `web` / `hybrid` / `external` |
| entryUri | string | 否 | 入口地址（native 可为 null） |
| version | object | 是 | `{major, minor, patch}` 语义版本 |
| minContainerVersion | int | 否 | 最低容器版本，默认 1 |
| permissions | string[] | 否 | 打开所需权限 |
| enabled | boolean | 是 | 当前用户是否启用 |
| sortOrder | int | 否 | 排序 |
| extension | object | 否 | JSON 安全的扩展配置 |
| extraConfig | object | 否 | 业务扩展配置 |

### 7.3 entryType 说明

| 值 | 说明 | Android 处理 |
|------|------|-------------|
| native | 原生 Kotlin Composable 页面 | `NativeTabPage` 按 route 分发到对应页面 |
| web | WebView 加载 URL | `WebTabPlaceholderPage` 展示 WebView |
| hybrid | 混合页面 | 当前不支持 |
| external | 外部跳转 | `ExternalTabPlaceholderPage` 展示占位 |

### 7.4 客户端已注册的 Native 路由

```kotlin
// OpenTabRegistry.nativeRoutes
"/approval"  → 审批中心
"/calendar"  → 团队日程
"/finance"   → 财务看板
"/ai-oncall" → AI OnCall 聊天
```

此外 `/docs`、`/bilibili`、`/custom-*` 也可识别但非 Native 路由。

### 7.5 Android 端 TabManifest 数据类

```kotlin
// open/model/TabManifest.kt
data class TabManifest(
    val id: String,
    val displayName: String,
    val description: String?,
    val icon: String?,
    val route: String,
    val entryType: EntryType,      // Native / Web / Hybrid / External
    val entryUri: String?,
    val version: SemanticVersionDto,
    val minContainerVersion: Int,
    val permissions: List<String>,
    val enabled: Boolean,
    val sortOrder: Int,
    val extension: TabExtensionDto?,
    val extraConfig: Map<String, String>
)
```

---

## 8. 业务 Tab 接入协议

### 8.1 核心参与者

- **容器**：负责 Tab 注册管理、页面切换、TitleBar 渲染、生命周期调度
- **接入方**：实现协议接口，提供页面 Composable、响应容器生命周期

### 8.2 Tab 元信息（TabDefinition）

```kotlin
@Stable
data class TabDefinition(
    val id: String,                    // 唯一标识，如 "com.example.content-audit"
    val displayName: String,           // 标题栏显示名称，≤ 16 字符
    val icon: ImageVector,             // BottomBar 图标，24dp×24dp
    val route: String,                 // 路由路径，"/" 开头
    val version: SemanticVersion,      // 协议版本号（major.minor.patch）
    val minContainerVersion: Int,      // 最低容器版本（默认 1）
    val permissions: List<String>,     // 所需权限
    val extension: TabExtension?       // 扩展点配置
)
```

### 8.3 生命周期（TabLifecycle）

```kotlin
interface TabLifecycle {
    fun onCreate(bundle: Bundle?)                    // Tab 首次创建
    fun onResume()                                   // Tab 可见（切换回来）
    fun onPause()                                    // Tab 不可见（切换走）
    fun onDestroy()                                  // Tab 被销毁
    fun onConfigChange(config: Configuration) = {}   // 可选：配置变更
}
```

状态机：`onCreate → onResume ↔ onPause → onDestroy`

调度规则：
1. 首次进入：onCreate → onResume
2. Tab 间切换：当前 onPause → 新 Tab onResume
3. 退出 Tab：onPause → onDestroy
4. 每个回调上限 5 秒，超时容器显示兜底页面
5. 回调抛异常不影响其他 Tab

### 8.4 UI 扩展点

```kotlin
data class TabExtension(
    val titleBar: TitleBarExtension?,   // TitleBar 右侧按钮/菜单
    val fab: FabExtension?,             // 悬浮按钮
    val bottomPanel: BottomPanel?       // v1.0 不支持
)
```

| 扩展点 | 默认行为 | 自定义后 |
|--------|---------|---------|
| TitleBar 右侧 | 无按钮 | 图标 / 文字 / 三点菜单 |
| TitleBar 标题 | displayName | 不可自定义 |
| FAB 按钮 | 无 | 图标 + 标签 |
| BottomPanel | 无 | v1.0 不支持 |

### 8.5 注册方式

静态注册（推荐，类型安全）：

```kotlin
val definition = TabDefinition(
    id = "com.example.content-audit",
    displayName = "内容审核",
    icon = Icons.Default.CheckCircle,
    route = "/content-audit",
    version = SemanticVersion(1, 0),
    minContainerVersion = 1,
    permissions = listOf("tab.audit.read"),
    extension = TabExtension(
        titleBar = TitleBarExtension(rightText = "筛选"),
        fab = FabExtension("create", Icons.Default.Add, "创建")
    )
)
```

动态注册（JSON 配置，服务端下发）：

```json
{
  "id": "com.example.dashboard",
  "displayName": "数据看板",
  "icon": "dashboard",
  "route": "/dashboard",
  "version": { "major": 1, "minor": 0 },
  "minContainerVersion": 1,
  "permissions": [],
  "extension": { "titleBar": { "rightText": "刷新" } }
}
```

### 8.6 路由规则

```
容器内路由格式: /{tab-route}?{params}
示例: /content-audit?filter=pending
```

Tab 注册时声明 `route` 前缀，容器匹配分发。查询参数透传。

### 8.7 版本兼容策略

```
major.minor.patch
major: 不兼容变更（容器必须升级）
minor: 新增可选字段（老容器忽略）
patch: 文档修正（无行为变化）
```

| 场景 | 容器行为 |
|------|---------|
| 容器版本 < Tab.minContainerVersion | 拒绝加载，提示"请升级客户端" |
| Tab 发送了容器不认识的字段 | 忽略多余字段 |
| 容器新增字段，Tab 未提供 | 使用默认值 |
| Tab 调用不存在的扩展点 | 静默忽略 |

### 8.8 错误码

| 错误码 | 含义 | 容器行为 |
|--------|------|---------|
| 1001 | Tab ID 重复注册 | 拒绝第二个，Toast 警告 |
| 1002 | minContainerVersion 不满足 | 拒绝加载，提示升级 |
| 1003 | Tab 缺失必填字段 | 拒绝加载 |
| 1004 | 生命周期回调超时（>5s） | 显示兜底页面 |
| 1005 | 生命周期回调抛出异常 | 显示兜底页面，打印日志 |
| 1006 | Tab 权限不足 | 拒绝加载，提示用户授权 |

### 8.9 接入流程（三步）

```
第一步：实现 TabLifecycle 接口，提供 Composable 页面
第二步：声明 TabDefinition 元信息
第三步：调用 Container.registerTab(definition, lifecycle, page)
```

### 8.10 v1.0 范围决议

| 议题 | 决议 |
|------|------|
| BottomBar 动态增加 Tab | v1.0 不支持，纳入 v2.0 |
| Tab 间数据共享 | v1.0 不支持，通过宿主 Activity/ViewModel 显式传参 |
| BottomPanel 扩展面板 | v1.0 不支持，v2.0 候选 |
| 通用能力（图片预览等） | 不纳入协议，由接入方自行实现 |

---

## 9. Android 客户端集成

### 9.1 AI OnCall 页面

```kotlin
// 在任意位置嵌入
OpenOnCallPage(
    modifier = Modifier.fillMaxSize(),
    repository = remember { OpenOnCallRepository() }
)
```

### 9.2 模块文件清单

| 文件 | 职责 |
|------|------|
| `open/config/OnCallConfig.kt` | AI Service URL、超时参数 |
| `open/config/OpenApiConfig.kt` | Container Server URL、演示账号 |
| `open/model/OpenApiModels.kt` | 数据模型 |
| `open/model/TabManifest.kt` | TabManifest 数据类 |
| `open/network/OnCallApiClient.kt` | AI Service 专用 HTTP 客户端 |
| `open/network/OpenApiClient.kt` | Container Server HTTP 客户端 |
| `open/repository/OpenOnCallRepository.kt` | SSE 事件解析 |
| `open/repository/OpenTabRepository.kt` | Tab 数据获取 |
| `open/ui/OpenOnCallPage.kt` | 聊天 UI 页面 |
| `open/ui/OpenTabContentHost.kt` | Tab 内容宿主 |
| `open/ui/OpenWorkbenchPage.kt` | 工作台页面 |
| `open/tab/OpenTabModels.kt` | OpenTabItem、状态、注册表 |

### 9.3 网络层设计

两个独立 HTTP 客户端：

```kotlin
// AI Service：port 8081，无鉴权
class OnCallApiClient(baseUrl = "http://121.40.241.161:8081") {
    // connectTimeout=30s, readTimeout=5min, writeTimeout=30s
    fun ssePost(path, json): Flow<OnCallSseLine>
    fun sseGet(path): Flow<OnCallSseLine>
}

// Container Server：port 8080，Bearer Token
class OpenApiClient(baseUrl = "http://121.40.241.161:8080") {
    // 通用 HTTP GET/POST，带 Token
}
```

### 9.4 事件解析映射

| SSE JSON type | Android 事件类 | Android 数据类 |
|---------------|---------------|----------------|
| sources | `OnCallStreamEvent.Sources` | `sources: List<String>` |
| intent | `OnCallStreamEvent.Intent` | `intent: String` |
| tool | `OnCallStreamEvent.Tool` | `OnCallToolEvent(name, status, summary)` |
| content | `OnCallStreamEvent.Delta` | `text: String` |
| done | `OnCallStreamEvent.Done` | `messageId: String?` |
| error | `OnCallStreamEvent.Error` | `code: String, message: String` |

### 9.5 UI 状态层（ViewModel）

```kotlin
class OnCallViewModel : BaseViewModel() {
    val uiState: StateFlow<OnCallUiState>

    fun sendMessage()        // 发送输入框消息
    fun sendQuickMessage()   // 发送快捷问题
    fun stopStreaming()      // 停止生成
    fun retryLastMessage()   // 重试
    fun clearConversation()  // 清空对话
}
```

页面通过 `collectAsState(uiState)` 订阅状态，按事件类型更新 `OnCallMessageUi`。

---

## 10. 部署运维

### 10.1 AI Service 部署

```bash
# 1. 构建
cd ai-service
./gradlew bootJar
# 产物: build/libs/ai-service-0.0.1-SNAPSHOT.jar

# 2. 启动（开发环境）
java -jar ai-service-0.0.1-SNAPSHOT.jar

# 3. 启动（生产环境，指定知识库目录）
java -DONCALL_DOCS_PATH=/opt/ai-oncall/docs -jar ai-service-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 4. 启动（生产环境 + 远程知识库自动拉取）
java -DONCALL_DOCS_PATH=/opt/ai-oncall/docs \
     -DONCALL_DOCS_REMOTE_URL=https://raw.example.com/ai-oncall-knowledge/manifest.txt \
     -jar ai-service-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### 10.2 知识库更新方式

| 方式 | 操作 | 适用场景 |
|------|------|---------|
| 本地文件 | SSH 上传 .md 到服务器 docs/，然后重启 | 临时更新 |
| 远程拉取 | 配置 ONCALL_DOCS_REMOTE_URL，重启即同步 | 团队成员协作更新 |
| 容器重启 | 使用新的 Docker 镜像 | 容器化部署 |

### 10.3 快速验证

```bash
curl -X POST http://localhost:8081/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"如何注册Tab","conversationId":"test"}'
```

验证通过条件：输出包含 `intent` → `tool` → `content` → `done` 的 SSE 事件流。

---

## 11. 常见问题排查

### 11.1 AI 回答不准确

**现象**：回答内容来自 LLM 而不是知识库。

**排查**：
1. 检查 `DocumentStore` 启动日志：确认 `"DocumentStore ready: N chunks from M files"`
2. 检查 DEBUG 日志中的搜索关键词：`"Search query='...', terms=[...]"`
3. 检查检索结果数：`"Document search: query=..., found=N"` — 如果 found=0，说明没有命中
4. 文档是否放在正确的路径？默认 `../docs`（相对于 jar 的执行目录）

**解决**：
- 知识库文档必须放在 `ONCALL_DOCS_PATH` 指定的目录
- 文件必须是 `.md` 格式
- 确保文档内容包含用户问题的关键词

### 11.2 SSE 连接断开

**现象**：Android 端收不到完整回答。

**排查**：
- 服务端日志是否有 `DeepSeek API error`
- DeepSeek API Key 是否有效
- 网络是否可达 `api.deepseek.com`
- `readTimeout=5min` 是否足够（LLM 长回答可能超过 5 分钟）

### 11.3 Android 端出现 "search running"

**现象**：工具卡片显示"search running"而不是"search done"。

**原因**：服务端 `ToolEvent` 发送时 `status` 字段缺失或被设为默认值。

**修复**：确认服务端 `ToolEvent` 包含 `status` 和 `summary` 字段。

### 11.4 Token 过期

**现象**：Android 端收到 `HTTP 401`。

**解决**：调用 `POST /auth/login` 重新登录获取 Token。

### 11.5 黑屏 / 显示异常

**现象**：Android 模拟器启动后黑屏。

**排查**：
1. 检查 GPU 渲染模式设置
2. 尝试 Cold Boot + Wipe Data
3. 切换为 Software 渲染

### 11.6 日志怎么看

- 服务端日志：`com.oncall.ai: DEBUG` 级别会输出检索详情
- Android 日志：过滤 `OnCallApiClient` / `OpenOnCallRepository` 查看 SSE 数据流

---

## 12. 扩展指南

### 12.1 替换 AI 厂商

只需修改 `DeepSeekClient.java`：

| 改动项 | 说明 |
|--------|------|
| API URL | `baseUrl + "/chat/completions"` 改为新厂商地址 |
| Header | 按厂商要求修改 `Authorization` |
| 响应解析 | 默认解析 `choices[0].delta.content`，按需调整 |

`ChatController`、`ChatEvent`、Android 协议全部不变。

### 12.2 添加新知识文档

1. 在 `docs/` 目录下新建 `.md` 文件
2. 使用 `##` 标题组织内容，每个 `##` 聚焦一个可回答的问题
3. 文件名包含关键词（如 `api-reference.md`）
4. 重启 ai-service，检查日志确认加载成功

### 12.3 添加新意图类型

1. 在 `IntentEvent.java` 新增常量（如 `CONFIG_GENERATION`）
2. 在 `DocumentAgentOrchestrator.detectIntent()` 添加检测规则
3. 如需对应新工具，在 `ToolEvent.java` 新增常量 + `detectTool()` 添加映射
4. Android 端 `OnCallViewModel.toDisplayIntent()` 添加中文标签

### 12.4 添加新事件类型

**服务端**：
1. 新增 `XxxEvent.java` 实现 `ChatEvent` 接口
2. 在 `ChatEvent.java` 的 `@JsonSubTypes` 中注册
3. Android 端 `OnCallStreamEvent` 新增子类
4. `OnCallViewModel.onXxx()` 处理新事件

### 12.5 远程知识库配置

在服务器上部署一个 Manifest 文件（plain text），内容为每行一个 `.md` 文件 URL：

```
https://raw.githubusercontent.com/xxx/project/main/docs/protocol-v1.0.md
https://raw.githubusercontent.com/xxx/project/main/docs/deployment-guide.md
```

然后通过 `ONCALL_DOCS_REMOTE_URL` 指向该 Manifest 地址。服务启动时会自动下载所有文档并覆盖本地文件。

---

> **本知识库由 AI OnCall 团队维护** | **提问时 AI OnCall 将严格依据本文档内容回答，不会使用 LLM 内置知识**