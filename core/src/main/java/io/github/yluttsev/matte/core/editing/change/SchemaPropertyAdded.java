package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.Schema;

public record SchemaPropertyAdded(
        ElementId schemaId,
        ElementId propertyId,
        String name,
        Schema schema
) implements Change {

    public SchemaPropertyAdded {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(name, "name");
        schema = Objects.requireNonNull(schema, "schema").copy();
    }
}
