package io.github.yluttsev.matte.core.model.media;

import io.github.yluttsev.matte.core.model.ElementId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RequestBody {

    private final ElementId id;
    private String description;
    private Boolean required;
    private final Map<String, MediaType> content = new LinkedHashMap<>();

    public RequestBody(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
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
}
