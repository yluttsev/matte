package io.github.yluttsev.matte.core.model.path;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.server.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PathItem {

    private final ElementId id;
    private String path;
    private String summary;
    private String description;
    private final Map<HttpMethod, Operation> operations = new LinkedHashMap<>();
    private final List<ReferenceOr<Parameter>> parameters = new ArrayList<>();
    private final List<Server> servers = new ArrayList<>();

    public PathItem(ElementId id, String path) {
        this.id = Objects.requireNonNull(id, "id");
        this.path = Objects.requireNonNull(path, "path");
    }

    public ElementId getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public Map<HttpMethod, Operation> getOperations() {
        return Collections.unmodifiableMap(operations);
    }

    public List<ReferenceOr<Parameter>> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public List<Server> getServers() {
        return Collections.unmodifiableList(servers);
    }
}
