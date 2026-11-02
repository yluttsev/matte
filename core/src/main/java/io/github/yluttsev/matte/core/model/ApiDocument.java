package io.github.yluttsev.matte.core.model;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.DocumentStructureValidator;
import io.github.yluttsev.matte.core.construction.ModelCopies;

import io.github.yluttsev.matte.core.model.info.ApiInfo;
import io.github.yluttsev.matte.core.model.info.ExternalDocumentation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.security.SecurityRequirement;
import io.github.yluttsev.matte.core.model.server.Server;
import io.github.yluttsev.matte.core.model.tag.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ApiDocument implements IdentifiedElement, CopyableModel<ApiDocument> {

    private final ElementId id;
    private ApiInfo info;
    private final List<Server> servers = new ArrayList<>();
    private final List<PathItem> paths = new ArrayList<>();
    private Components components = new Components();
    private final List<Tag> tags = new ArrayList<>();
    private final List<SecurityRequirement> security = new ArrayList<>();
    private ExternalDocumentation externalDocs;

    public ApiDocument(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private ApiDocument(ApiDocument source) {
        this(source.id);
        info = ModelCopies.copy(source.info);
        servers.addAll(ModelCopies.list(source.servers));
        paths.addAll(ModelCopies.list(source.paths));
        components = source.components.copy();
        tags.addAll(ModelCopies.list(source.tags));
        security.addAll(ModelCopies.list(source.security));
        externalDocs = source.externalDocs;
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public ApiDocument copy() {
        return new ApiDocument(this);
    }

    public ElementId getId() {
        return id;
    }

    public Components getComponents() {
        return components;
    }

    public ApiInfo getInfo() {
        return info;
    }

    public List<Server> getServers() {
        return Collections.unmodifiableList(servers);
    }

    public List<PathItem> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public List<SecurityRequirement> getSecurity() {
        return Collections.unmodifiableList(security);
    }

    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    public static final class Builder {

        private final ApiDocument draft;

        private Builder(ElementId id) {
            draft = new ApiDocument(id);
        }

        public Builder info(ApiInfo info) {
            draft.info = info;
            return this;
        }

        public Builder servers(List<Server> servers) {
            List<Server> values = List.copyOf(servers);
            draft.servers.clear();
            draft.servers.addAll(values);
            return this;
        }

        public Builder paths(List<PathItem> paths) {
            List<PathItem> values = List.copyOf(paths);
            draft.paths.clear();
            draft.paths.addAll(values);
            return this;
        }

        public Builder components(Components components) {
            draft.components = Objects.requireNonNull(components, "components");
            return this;
        }

        public Builder tags(List<Tag> tags) {
            List<Tag> values = List.copyOf(tags);
            draft.tags.clear();
            draft.tags.addAll(values);
            return this;
        }

        public Builder security(List<SecurityRequirement> security) {
            List<SecurityRequirement> values = List.copyOf(security);
            draft.security.clear();
            draft.security.addAll(values);
            return this;
        }

        public Builder externalDocs(ExternalDocumentation externalDocs) {
            draft.externalDocs = externalDocs;
            return this;
        }

        public ApiDocument build() {
            DocumentStructureValidator.validate(draft);
            return draft.copy();
        }
    }
}
