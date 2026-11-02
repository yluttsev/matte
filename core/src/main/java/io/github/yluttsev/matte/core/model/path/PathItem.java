package io.github.yluttsev.matte.core.model.path;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.server.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PathItem implements IdentifiedElement, CopyableModel<PathItem> {

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

    private PathItem(PathItem source) {
        this(source.id, source.path);
        summary = source.summary;
        description = source.description;
        operations.putAll(ModelCopies.map(source.operations));
        parameters.addAll(ModelCopies.list(source.parameters));
        servers.addAll(ModelCopies.list(source.servers));
    }

    public static Builder builder(ElementId id, String path) {
        return new Builder(id, path);
    }

    @Override
    public PathItem copy() {
        return new PathItem(this);
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

    public static final class Builder {

        private final PathItem draft;

        private Builder(ElementId id, String path) {
            draft = new PathItem(id, path);
        }

        public Builder summary(String summary) {
            draft.summary = summary;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder operations(Map<HttpMethod, Operation> operations) {
            Map<HttpMethod, Operation> values = ModelCopies.valueMap(operations);
            draft.operations.clear();
            draft.operations.putAll(values);
            return this;
        }

        public Builder parameters(List<ReferenceOr<Parameter>> parameters) {
            List<ReferenceOr<Parameter>> values = List.copyOf(parameters);
            draft.parameters.clear();
            draft.parameters.addAll(values);
            return this;
        }

        public Builder servers(List<Server> servers) {
            List<Server> values = List.copyOf(servers);
            draft.servers.clear();
            draft.servers.addAll(values);
            return this;
        }

        public PathItem build() {
            return draft.copy();
        }
    }
}
