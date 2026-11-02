package io.github.yluttsev.matte.core.model.media;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

import java.util.Objects;

public final class Example {

    private final ElementId id;
    private String summary;
    private String description;
    private DocumentValue value;
    private String externalValue;

    public Example(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public ElementId getId() {
        return id;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public DocumentValue getValue() {
        return value;
    }

    public String getExternalValue() {
        return externalValue;
    }
}
