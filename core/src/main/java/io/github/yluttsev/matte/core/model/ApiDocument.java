package io.github.yluttsev.matte.core.model;

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

public final class ApiDocument {

    private final ElementId id;
    private ApiInfo info;
    private final List<Server> servers = new ArrayList<>();
    private final List<PathItem> paths = new ArrayList<>();
    private final Components components = new Components();
    private final List<Tag> tags = new ArrayList<>();
    private final List<SecurityRequirement> security = new ArrayList<>();
    private ExternalDocumentation externalDocs;

    public ApiDocument(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
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
}
