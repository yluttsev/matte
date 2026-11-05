package io.github.yluttsev.matte.core.editing.command;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;

public record DeleteSchemaProperty(
        ElementId schemaId,
        ElementId propertyId
) implements EditCommand {

    public DeleteSchemaProperty {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(propertyId, "propertyId");
    }
}
