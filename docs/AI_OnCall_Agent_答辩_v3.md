定位
- 不是聊天机器人。是一个具备 RAG 检索 + 工具调用 + 上下文记忆 + 流式输出 的 Agent。
- 不是套用大模型的躯壳ai，不是只会调用外部api，适用于app内部开发人员使用。
亮点
RAG 知识库
普通聊天机器人最大的问题是幻觉——你问它协议字段，它可能编造。我们搭建了 BM25 检索的知识库，43 份协议文档，中文分词，设定 0.5 阈值过滤低质量结果。
flowchart TB
    A["用户提问"] --> B["Intent 识别"]
    B --> C["SearchTool → BM25 检索"]
    C --> D{"hit 判定<br/>score ≥ 0.5"}
    D -->|"hit=true"| E["注入知识库原文 + 强制引用来源"]
    D -->|"hit=false"| F["标注『知识库未命中』 → LLM 兜底"]
    E --> G["SourcesEvent 展示来源"]
    F --> G
    G --> H["LLM 流式生成答案"]
修复前
修复后
"未命中" 同时 "命中 5 篇"
hit=false → sourceItems 一定为空
得分永远显示 0.00
BM25 真实得分 0~50+
LLM 自己判断是否命中
代码层硬约束，切换 Prompt 模板
错误日志自动诊断
- 开发者遇到异常要去查找解决，我内置了 7 种异常的正则匹配——NPE、SSL、超时、SQL、黑屏等。用户粘贴一段堆栈，Agent 自动识别异常类型、匹配知识库中的排查文档、输出「定位—根因—修复—预防」四步方案。如果 7 种都没命中，BM25 回退兜底。这不是让 AI 猜，而是结构化的诊断流程。
flowchart TB
    A["用户粘贴堆栈日志"] --> B["Intent → ERROR_DIAGNOSIS"]
    B --> C["AnalyzeLogTool 执行"]
    C --> D{"正则匹配 7 种异常"}
    D -->|"命中 NPE/SSL/Timeout/SQL/黑屏/SSE"| E["提取排查文档 → confidence=0.85"]
    D -->|"未命中"| F["BM25 检索回退"]
    E --> G["注入 Prompt：定位+根因+修复+预防"]
    F --> G
三层 Memory
- 普通对话是无状态的——用户先问「Tab 怎么注册」，再问「那生命周期呢」——普通 AI 不知道「那」指什么。我们设计了三层 Memory：Working Memory 记录本轮工具和文档、Conversation Memory 保留最近 20 轮、超过 10 轮时自动触发 Summary Memory 把历史压缩成 50 字摘要。这样既保持了上下文连续性，又控制了 Token 消耗，比全量历史节省约 60%。
flowchart TB
    subgraph L3["Working Memory（本轮）"]
        W["lastIntent / recentTools / recentSources"]
    end
    subgraph L1["Conversation Memory（滚动 20 轮）"]
        C["最近 20 轮完整消息，注入 Prompt 取最近 10 轮"]
    end
    subgraph L2["Summary Memory（压缩）"]
        S["超过 10 轮自动触发摘要 → 50 字中文摘要"]
    end
    L3 --> L1
    L1 --> L2
    L2 --> P["buildMemoryContext() → 注入 System Prompt"]
SSE 流式 + 7 层防御
- 我设计了 5 种 SSE 事件——意图、工具、来源、内容、完成——从 Android 发起请求到 DeepSeek 逐 Token 返回，全链路可控。做了 7 层防御：心跳 15 秒保活、messageId 去重防止重复回答、断线后指数退避重连、三层超时控制、错误兜底。这些保证流式输出的稳定性。
7 层防御：messageId 去重 → 心跳 15s → 指数退避重连 (1s→2s→4s) → connectTimeout 30s / readTimeout 5min → ErrorEvent 兜底 → RETRY 事件识别 → buffer(8) 平滑
sequenceDiagram
    participant A as Android
    participant C as ChatController
    participant O as Orchestrator
    participant D as DeepSeek
    A->>C: POST /api/chat/stream
    C->>C: messageId 去重
    C->>O: stream()
    O->>A: intent event
    O->>A: tool event
    O->>A: sources event
    loop 逐 token
        D-->>O: delta
        O-->>A: content event
    end
    O-->>A: done event
    loop 每 15s
        C-->>A: 心跳 keepalive
    end
工具调用
- 每个工具返回统一的 `ToolResult`（含 summary + sources + detailData + confidence），Orchestrator 不关心工具内部实现，只对接接口。
flowchart TB subgraph Orchestrator["DocumentAgentOrchestrator"] INTENT["detectIntent()"] MAP["intentToTool()"] end subgraph Tools["工具实现"] SEARCH["SearchTool\nBM25 检索 · 置信度计算"] ANALYZE["AnalyzeLogTool\n7 种异常正则匹配\n+ BM25 回退"] GENERATE["GenerateCodeTool\ncode-template 检索\n· 模板拼接"] end INTENT --> MAP MAP -->|PROTOCOL_QA| SEARCH MAP -->|ERROR_DIAGNOSIS| ANALYZE MAP -->|CODE_GENERATION| GENERATE MAP -->|GENERAL_CHAT| SKIP["跳过工具 · 直接 LLM"]