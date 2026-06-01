package com.oncall.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"type", "delta"})
public record ContentEvent(String delta) implements ChatEvent {

    @Override
    public String type() {
        return "content";
    }
}
