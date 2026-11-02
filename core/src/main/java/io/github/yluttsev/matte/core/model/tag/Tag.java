package io.github.yluttsev.matte.core.model.tag;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.info.ExternalDocumentation;

import java.util.Objects;

public final class Tag {

    private final ElementId id;
    private String name;
    private String description;
    private ExternalDocumentation externalDocs;

    public Tag(ElementId id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
    }

    public ElementId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }
}
