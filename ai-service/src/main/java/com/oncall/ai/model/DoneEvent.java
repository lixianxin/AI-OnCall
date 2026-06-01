package com.oncall.ai.model;

public record DoneEvent(String messageId) implements ChatEvent {

    @Override
    public String type() {
        return "done";
    }

    public DoneEvent() {
        this(null);
    }
}
