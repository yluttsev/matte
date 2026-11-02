package io.github.yluttsev.matte.core.model.media;

import io.github.yluttsev.matte.core.construction.CopyableModel;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

import java.util.Objects;

public final class Example implements IdentifiedElement, CopyableModel<Example> {

    private final ElementId id;
    private String summary;
    private String description;
    private DocumentValue value;
    private String externalValue;

    public Example(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private Example(Example source) {
        this(source.id);
        summary = source.summary;
        description = source.description;
        value = source.value;
        externalValue = source.externalValue;
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public Example copy() {
        return new Example(this);
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

    public static final class Builder {

        private final Example draft;

        private Builder(ElementId id) {
            draft = new Example(id);
        }

        public Builder summary(String summary) {
            draft.summary = summary;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder value(DocumentValue value) {
            draft.value = value;
            return this;
        }

        public Builder externalValue(String externalValue) {
            draft.externalValue = externalValue;
            return this;
        }

        public Example build() {
            return draft.copy();
        }
    }
}
