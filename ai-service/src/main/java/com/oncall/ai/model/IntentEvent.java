package com.oncall.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"type", "intent"})
public record IntentEvent(String intent) implements ChatEvent {

    @Override
    public String type() {
        return "intent";
    }

    /** 预定义的意图类型常量 */
    public static final String PROTOCOL_QA = "PROTOCOL_QA";
    public static final String ERROR_DIAGNOSIS = "ERROR_DIAGNOSIS";
    public static final String CODE_GENERATION = "CODE_GENERATION";
    public static final String GENERAL_CHAT = "GENERAL_CHAT";
}
