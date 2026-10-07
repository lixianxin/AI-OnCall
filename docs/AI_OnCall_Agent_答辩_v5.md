# AI OnCall Agent — 答辩讲稿

> 负责：AI Agent 后端全链路 | 技术：Spring Boot WebFlux + DeepSeek + BM25 + SSE

---

## 一句话说清楚我做了什么

AI OnCall 是嵌在 OpenTab 容器里的开发助手。和普通聊天框的本质区别是：**它不靠大模型"猜"答案，而是从 43 份协议文档里检索原文来回答。**

举个例子——开发者问"TabManifest 必填字段有哪些"：

普通 AI 会编：`name、title、url` — 这些字段 OpenTab 协议里根本不存在。

我的 Agent 回答：`id（反向域名格式）、displayName（≤16字符）、route（/开头）、entryType、version` — 每个字段都来自 `tab-manifest.md` 的字段约束表，右侧有个来源卡片可以点开验证。

---

## 亮点一：RAG 知识库 — 回答必须出自协议文档

**为什么这很重要。** OpenTab 协议有精确定义：TabDefinition 6 个字段、TabManifest 13 个字段、6 种错误码（1001~1006）。大模型不知道这些，问它就会编。开发者照着编的字段写代码，上线注册直接 1003 报错。

**我是怎么做的。** 43 份 Markdown 按 `##` 标题切块，中文 bigram 分词，BM25 阈值 0.5。关键设计在于命中判定——不是简单看 BM25 得分，而是双条件：得分 ≥0.5 且 bigram 覆盖率 ≥20%。这是踩过坑才加上去的。最开始只有得分判定，"你好"这种问候语也能命中 5 篇文档，因为单个汉字会被当做 token 匹配到几乎所有 chunk。

还有个更隐蔽的 Bug：前端同时显示"知识库未命中"和"命中文档 5 篇、得分 8.97"。排查发现 `hit` 判定用的是 `hasKeywordOverlap()`，`SourcesEvent` 构建用的是 `searchWithScores()` 的未过滤结果——同一份数据，两套逻辑。改成一处的 `validResults` 驱动两处，才彻底消掉矛盾。

**效果。** `hit=true` → 注入协议原文，强制 LLM 引用来源；`hit=false` → 标注"知识库未命中"走 LLM 兜底，SourcesEvent 为空。两条路径泾渭分明。

---

## 亮点二：错误日志诊断 — 粘贴堆栈直接出定位和修复

**为什么这有用。** 开发者遇到 NPE → 复制堆栈 → 百度 → CSDN → 试错，一轮下来半小时起步。而且搜出来的答案跟 OpenTab 项目无关，只能泛泛说"加判空"。

**我怎么做的。** `AnalyzeLogTool` 内置 7 种异常的正则匹配，每种都关联了本项目真实会出现问题的位置：

- NPE → 指向 `DocumentStore.loadFile()` 目录不存在、`DeepSeekClient.extractDeltaContent()` 中 JSON 解析为 null
- SSL → 指向自签名证书连不上 DevServer 的场景
- 1003 → 指向 Tab 缺少必填字段，关联 `tab-error-code.md`
- 超时、SQL、黑屏、SSE 断开也都有对应正则和文档

匹配上就关联知识库排查文档，匹配不上就走 BM25 检索回退。最后统一注入 Prompt，让 LLM 输出"定位 → 根因 → 修复 → 预防"四步。

**不是让 AI 猜，而是结构化诊断。** 这和直接把异常丢给 DeepSeek 有本质区别——我们先做了异常分类和文档关联，LLM 只是最后一步的"语言组织器"。

---

## 亮点三：上下文记忆 — 追问时 Agent 知道你在说什么

**最直观的例子：**

```
开发者：Tab 怎么注册？
AI：    基于 tab-register.md，需要声明 TabDefinition...
开发者：那生命周期呢？
```

普通 AI 不知道"那"指什么，只能重新讲一遍泛泛的 Android 生命周期。我的 Agent 知道"那"指的是 Tab，因为上一轮的 Working Memory 里记录了 `recentSources=[tab-register.md]`。

**怎么实现的。** 三层 Memory：

第一层 Working Memory，记录本轮的 intent、用过的工具、引用的文档。追问时这些信息自动注入 Prompt。

第二层 Conversation Memory，滚动保留最近 20 轮完整消息，但只把最近 10 轮注入 Prompt——不是无脑全量。

第三层 Summary Memory，超过 10 轮对话时自动触发，调用 DeepSeek 把历史压缩成 50 字摘要，替换掉早期消息。这样 Timeline 再长，Token 消耗也稳定——比全量历史省约 60%。

---

## SSE 流式 + 防御（简要说）

5 种事件：intent → tool → sources → content → done，从 Android 发起到 DeepSeek 逐 Token 返回全程可控。

做了 7 层防御，说关键的 3 个：

1. **messageId 去重**：每个请求带唯一 ID，服务端 `ConversationStore` 用 `ConcurrentHashMap` 缓存已处理的 ID，重试消息直接返回 `DUPLICATE_MESSAGE`，不会重复回答
2. **指数退避重连**：OkHttp 层 while 循环最多 3 次，间隔 1s → 2s → 4s，且重连期间发的 `RETRY_` 事件不影响 UI
3. **buffer(8) 平滑**：Kotlin Flow 加 8 行缓冲，网络抖动时数据先入缓冲区再逐条下发，UI 不闪烁

---

## 总结

跟普通 DeepSeek 聊天框的区别不是多几个功能模块，而是核心逻辑变了：

- 普通聊天：用户问 → 大模型猜 → 输出
- AI OnCall：用户问 → 检索协议文档 → 判定命中 → 有依据就引用原文，没有就明确告知 → LLM 组织语言 → 来源可追溯

知识库可追溯、工具可调用、上下文可记忆、流式可防御——这是 Agent，不是套壳。
