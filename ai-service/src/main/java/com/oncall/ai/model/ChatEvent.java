package com.oncall.ai.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 统一的 AI 流式事件抽象（Android 协议边界）
 *
 * 与 Android 端 OnCallStreamEvent 一一对应。
 * 后续 DeepSeek Agent 输出不再需要修改协议层。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = SourcesEvent.class, name = "sources"),
    @JsonSubTypes.Type(value = IntentEvent.class, name = "intent"),
    @JsonSubTypes.Type(value = ToolEvent.class, name = "tool"),
    @JsonSubTypes.Type(value = ContentEvent.class, name = "content"),
    @JsonSubTypes.Type(value = DoneEvent.class, name = "done"),
    @JsonSubTypes.Type(value = ErrorEvent.class, name = "error")
})
public sealed interface ChatEvent
        permits IntentEvent, ToolEvent, ContentEvent, DoneEvent, ErrorEvent, SourcesEvent {

    String type();
}
