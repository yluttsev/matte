package io.github.yluttsev.matte.core.model.response;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ApiResponse {

    private final ElementId id;
    private String description;
    private final Map<String, ReferenceOr<Header>> headers = new LinkedHashMap<>();
    private final Map<String, MediaType> content = new LinkedHashMap<>();

    public ApiResponse(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
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
}
