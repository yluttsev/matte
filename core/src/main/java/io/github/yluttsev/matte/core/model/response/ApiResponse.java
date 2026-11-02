package io.github.yluttsev.matte.core.model.response;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ApiResponse implements IdentifiedElement, CopyableModel<ApiResponse> {

    private final ElementId id;
    private String description;
    private final Map<String, ReferenceOr<Header>> headers = new LinkedHashMap<>();
    private final Map<String, MediaType> content = new LinkedHashMap<>();

    public ApiResponse(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private ApiResponse(ApiResponse source) {
        this(source.id);
        description = source.description;
        headers.putAll(ModelCopies.map(source.headers));
        content.putAll(ModelCopies.map(source.content));
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public ApiResponse copy() {
        return new ApiResponse(this);
    }

    public ElementId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, ReferenceOr<Header>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public Map<String, MediaType> getContent() {
        return Collections.unmodifiableMap(content);
    }

    public static final class Builder {

        private final ApiResponse draft;

        private Builder(ElementId id) {
            draft = new ApiResponse(id);
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder headers(Map<String, ReferenceOr<Header>> headers) {
            Map<String, ReferenceOr<Header>> values = ModelCopies.valueMap(headers);
            draft.headers.clear();
            draft.headers.putAll(values);
            return this;
        }

        public Builder content(Map<String, MediaType> content) {
            Map<String, MediaType> values = ModelCopies.valueMap(content);
            draft.content.clear();
            draft.content.putAll(values);
            return this;
        }

        public ApiResponse build() {
            return draft.copy();
        }
    }
}
