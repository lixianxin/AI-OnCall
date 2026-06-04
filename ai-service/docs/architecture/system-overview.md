# AI OnCall 系统架构

## 项目文件结构

`
D:\AI-OnCall/
├── ai-service/                          # AI 服务端（Spring Boot WebFlux）
│   ├── src/main/java/com/oncall/ai/
│   │   ├── AiServiceApplication.java     # 启动入口
│   │   ├── config/WebFluxConfig.java     # CORS 跨域配置
│   │   ├── controller/ChatController.java # POST /api/chat/stream
│   │   ├── model/                        # SSE 事件模型（6种）
│   │   │   ├── ChatEvent.java            # sealed interface
│   │   │   ├── IntentEvent.java          # 意图事件
│   │   │   ├── ToolEvent.java            # 工具事件
│   │   │   ├── ContentEvent.java         # 内容增量事件
│   │   │   ├── SourcesEvent.java         # 来源文档事件
│   │   │   ├── DoneEvent.java            # 流结束事件
│   │   │   ├── ErrorEvent.java           # 错误事件
│   │   │   ├── ChatRequest.java          # 请求 DTO
│   │   │   ├── Conversation.java         # 会话模型
│   │   │   └── Message.java              # 消息实体
│   │   └── service/
│   │       ├── DocumentAgentOrchestrator.java  # RAG 编排主流程
│   │       ├── DocumentStore.java              # BM25 检索引擎
│   │       ├── DeepSeekClient.java             # LLM 流式调用
│   │       ├── ConversationStore.java          # 会话管理
│   │       ├── ToolExecutor.java               # 工具执行接口
│   │       ├── ToolContext.java                # 工具上下文
│   │       ├── ToolResult.java                 # 工具执行结果
│   │       ├── ToolExecutorFactory.java        # 工具工厂
│   │       ├── SearchTool.java                 # 知识库搜索工具
│   │       └── AnalyzeLogTool.java             # 日志分析工具
│   ├── docs/                             # RAG 知识库目录
│   └── build/libs/ai-service.jar         # 构建产物
│
├── client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/
│   └── open/
│       ├── config/OnCallConfig.kt        # AI Service 配置（端口 8081）
│       ├── config/OpenApiConfig.kt       # Container Server 配置（端口 8080）
│       ├── model/OpenApiModels.kt        # 数据模型
│       ├── model/TabManifest.kt          # TabManifest 数据类
│       ├── network/OnCallApiClient.kt    # AI Service HTTP 客户端
│       ├── network/OpenApiClient.kt      # Container Server HTTP 客户端
│       ├── repository/OpenOnCallRepository.kt  # SSE 事件解析
│       ├── repository/OpenTabRepository.kt     # Tab 数据获取
│       ├── ui/OpenOnCallPage.kt          # 聊天 UI 页面
│       ├── ui/OpenTabContentHost.kt      # Tab 内容宿主
│       ├── ui/OpenWorkbenchPage.kt       # 工作台页面
│       ├── tab/OpenTabModels.kt          # Tab 模型与注册表
│       └── session/OpenSessionManager.kt # 会话管理
│
├── protocol/                             # 协议定义（当前为占位）
└── server/                               # Container Mock Server（当前为占位）
`

## 部署架构

`
┌─ 客户端 ─────────────────────────────────────┐
│  Android Emulator / 真机                       │
│  OpenOnCallPage (Compose)                     │
│    ↓                                         │
│  OnCallApiClient (OkHttp + SSE)              │
└──────────────┬───────────────────────────────┘
               │
        ┌──────┴──────┐
        ▼             ▼
┌──────────────┐ ┌──────────────┐
│ ai-service   │ │ Container    │
│ :8081        │ │ Server(Mock) │
│ AI 聊天      │ │ :8080        │
│ RAG 检索     │ │ 登录/鉴权    │
│ SSE 流式输出  │ │ Tab CRUD     │
└──────────────┘ └──────────────┘
`

## 数据流

用户提问 → SSE 事件流的完整链路：

`
用户输入 "如何注册Tab"
  ↓
OnCallApiClient.ssePost() → POST /api/chat/stream
  ↓
ChatController.streamChat()
  ↓
DocumentAgentOrchestrator.stream()
  ├─ detectIntent("如何注册Tab") → "PROTOCOL_QA"
  ├─ ToolExecutorFactory.create("search") → SearchTool
  ├─ SearchTool.execute() → DocumentStore.search() → Top-5 chunks
  └─ 拼接事件流:
      IntentEvent("PROTOCOL_QA")
      → SourcesEvent(["protocol/tab-register.md"])
      → ToolEvent("search", "done", "已检索到 3 条相关文档")
      → ContentEvent("注册Tab需要...") × N
      → DoneEvent(messageId)
  ↓
ChatController → SSE 序列化 → event: message
  ↓
OnCallApiClient → 逐行读取 → OpenOnCallRepository.parseSseLine()
  ↓
OnCallViewModel.collect() → update UiState
  ↓
OpenOnCallPage recompose → 流式渲染
`

## 相关来源

api/ai-service-api.md
api/sse-protocol.md