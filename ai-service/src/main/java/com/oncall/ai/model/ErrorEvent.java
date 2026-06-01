package com.oncall.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"type", "code", "delta"})
public record ErrorEvent(String code, String delta) implements ChatEvent {

    @Override
    public String type() {
        return "error";
    }
}
