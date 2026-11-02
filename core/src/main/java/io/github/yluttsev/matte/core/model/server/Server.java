package io.github.yluttsev.matte.core.model.server;

import io.github.yluttsev.matte.core.model.ElementId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Server {

    private final ElementId id;
    private String url;
    private String description;
    private final Map<String, ServerVariable> variables = new LinkedHashMap<>();

    public Server(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public ElementId getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, ServerVariable> getVariables() {
        return Collections.unmodifiableMap(variables);
    }
}
