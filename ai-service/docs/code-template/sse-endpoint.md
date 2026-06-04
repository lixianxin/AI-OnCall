# 生成 SSE 端点模板

## 用途

生成 Server-Sent Events 流式端点，适用于 AI OnCall 这类流式响应场景。

## 模板

```java
@RestController
public class StreamController {

    @PostMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest request) {
        return Flux.create(sink -> {
            // 模拟流式输出
            for (int i = 0; i < 5; i++) {
                sink.next(ServerSentEvent.<String>builder()
                    .event("message")
                    .data("{\"type\":\"content\",\"delta\":\"第" + (i+1) + "段输出\"}")
                    .build());
            }
            sink.complete();
        });
    }
}
```

## SSE 协议格式

服务端统一使用 `event: message`，类型由 JSON 的 `type` 字段区分：

| type | 说明 |
|------|------|
| sources | 来源文档列表 |
| intent | 意图识别结果 |
| tool | 工具调用 |
| content | AI 增量文本 |
| done | 流结束 |
| error | 错误信息 |

## 相关来源

api > sse-protocol.md