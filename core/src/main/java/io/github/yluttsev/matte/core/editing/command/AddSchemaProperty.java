package io.github.yluttsev.matte.core.editing.command;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;

public record AddSchemaProperty(ElementId schemaId, SchemaProperty property) implements EditCommand {

    public AddSchemaProperty {
        Objects.requireNonNull(schemaId, "schemaId");
        property = Objects.requireNonNull(property, "property").copy();
    }
}
