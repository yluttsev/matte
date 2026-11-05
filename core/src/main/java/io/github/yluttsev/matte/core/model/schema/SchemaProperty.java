package io.github.yluttsev.matte.core.model.schema;

import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.editing.MutationAccess;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;

public final class SchemaProperty implements IdentifiedElement, CopyableModel<SchemaProperty> {

    private final ElementId id;
    private String name;
    private Schema schema;

    public SchemaProperty(ElementId id, String name, Schema schema) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.schema = Objects.requireNonNull(schema, "schema").copy();
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

    public void rename(MutationAccess access, String name) {
        access.require(this);
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public SchemaProperty copy() {
        return new SchemaProperty(id, name, schema);
    }
}
