package io.github.yluttsev.matte.core.model.tag;

import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.info.ExternalDocumentation;

public final class Tag implements IdentifiedElement, CopyableModel<Tag> {

    private final ElementId id;
    private String name;
    private String description;
    private ExternalDocumentation externalDocs;

    public Tag(ElementId id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
    }

    private Tag(Tag source) {
        this(source.id, source.name);
        description = source.description;
        externalDocs = source.externalDocs;
    }

    public static Builder builder(ElementId id, String name) {
        return new Builder(id, name);
    }

    @Override
    public Tag copy() {
        return new Tag(this);
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

    public static final class Builder {

        private final Tag draft;

        private Builder(ElementId id, String name) {
            draft = new Tag(id, name);
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder externalDocs(ExternalDocumentation externalDocs) {
            draft.externalDocs = externalDocs;
            return this;
        }

        public Tag build() {
            return draft.copy();
        }
    }
}
