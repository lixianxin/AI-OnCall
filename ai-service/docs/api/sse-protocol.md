# SSE 事件协议参考

## 协议定义

服务端统一使用 event: message 发送所有事件，事件类型由 JSON 中的 	ype 字段区分。

**源码位置**：
- 服务端：i-service/src/main/java/com/oncall/ai/model/ (ChatEvent sealed interface + 6 种实现)
- Android：client-android/.../open/repository/OpenOnCallRepository.kt (OnCallStreamEvent sealed class)

## 事件顺序

`
sources → intent → tool → content × N → done
`

## 6 种事件格式

### sources — 来源文档

`json
{"type":"sources","sources":["protocol/tab-register.md"]}
`
服务端类：SourcesEvent.java → Android 类：OnCallStreamEvent.Sources

### intent — 意图

`json
{"type":"intent","intent":"PROTOCOL_QA"}
`
服务端类：IntentEvent.java → Android 类：OnCallStreamEvent.Intent

意图值：PROTOCOL_QA / ERROR_DIAGNOSIS / CODE_GENERATION / GENERAL_CHAT

### tool — 工具调用

`json
{"type":"tool","tool":"search","status":"done","summary":"已检索知识库，找到 5 条相关文档片段"}
`
服务端类：ToolEvent.java → Android 类：OnCallStreamEvent.Tool

工具值：search / analyze_log / generate

### content — AI 增量文本

`json
{"type":"content","delta":"注册Tab需要完成以下三步：\\n\\n1. 实现 TabLifecycle 接口..."}
`
服务端类：ContentEvent.java → Android 类：OnCallStreamEvent.Delta

### done — 流结束

`json
{"type":"done","messageId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890"}
`
服务端类：DoneEvent.java → Android 类：OnCallStreamEvent.Done

### error — 错误

`json
{"type":"error","code":"STREAM_ERROR","delta":"DeepSeek API returned 401: invalid api key"}
`
服务端类：ErrorEvent.java → Android 类：OnCallStreamEvent.Error

## Android 端解析映射

`kotlin
// OpenOnCallRepository.parseSseLine()
when (obj.optString("type")) {
    ""sources"" → OnCallStreamEvent.Sources(sources = list)
    ""intent""  → OnCallStreamEvent.Intent(intent = ...)
    ""tool""    → OnCallStreamEvent.Tool(tool = OnCallToolEvent(name, status, summary))
    ""content"" → OnCallStreamEvent.Delta(text = ...)
    ""done""    → OnCallStreamEvent.Done(messageId = null)
    ""error""   → OnCallStreamEvent.Error(code = ..., message = ...)
}
`

## 服务端实现要点

- ChatController 使用 @PostMapping(produces = TEXT_EVENT_STREAM_VALUE)
- ChatEvent 是 sealed interface，Jackson @JsonTypeInfo 自动注入 type 字段
- 所有事件统一用 event: message 发送

## 相关来源

architecture/system-overview.md
ai-service-api.md