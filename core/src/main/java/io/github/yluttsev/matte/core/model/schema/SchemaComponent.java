package io.github.yluttsev.matte.core.model.schema;

import io.github.yluttsev.matte.core.model.ElementId;

import java.util.Objects;

public final class SchemaComponent {

    private final ElementId id;
    private String name;
    private Schema schema;

    public SchemaComponent(ElementId id, String name, Schema schema) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.schema = Objects.requireNonNull(schema, "schema");
    }

    public ElementId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Schema getSchema() {
        return schema;
    }
}
