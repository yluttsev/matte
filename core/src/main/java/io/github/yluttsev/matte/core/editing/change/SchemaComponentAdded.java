package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.Schema;

public record SchemaComponentAdded(ElementId componentId, String name, Schema schema) implements Change {

    public SchemaComponentAdded {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(name, "name");
        schema = Objects.requireNonNull(schema, "schema").copy();
    }
}
