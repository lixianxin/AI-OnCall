package com.oncall.ai.model;

import java.util.List;

public record SourcesEvent(List<SourceItem> sources) implements ChatEvent {

    @Override
    public String type() {
        return "sources";
    }

    public SourcesEvent() {
        this(List.of());
    }

    public record SourceItem(
        String file,
        String section,
        double relevance,
        String snippet
    ) {}
}
