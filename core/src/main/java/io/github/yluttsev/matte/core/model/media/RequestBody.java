package io.github.yluttsev.matte.core.model.media;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RequestBody implements IdentifiedElement, CopyableModel<RequestBody> {

    private final ElementId id;
    private String description;
    private Boolean required;
    private final Map<String, MediaType> content = new LinkedHashMap<>();

    public RequestBody(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private RequestBody(RequestBody source) {
        this(source.id);
        description = source.description;
        required = source.required;
        content.putAll(ModelCopies.map(source.content));
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public RequestBody copy() {
        return new RequestBody(this);
    }

    public ElementId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getRequired() {
        return required;
    }

    public Map<String, MediaType> getContent() {
        return Collections.unmodifiableMap(content);
    }

    public static final class Builder {

        private final RequestBody draft;

        private Builder(ElementId id) {
            draft = new RequestBody(id);
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder required(Boolean required) {
            draft.required = required;
            return this;
        }

        public Builder content(Map<String, MediaType> content) {
            Map<String, MediaType> values = ModelCopies.valueMap(content);
            draft.content.clear();
            draft.content.putAll(values);
            return this;
        }

        public RequestBody build() {
            return draft.copy();
        }
    }
}
