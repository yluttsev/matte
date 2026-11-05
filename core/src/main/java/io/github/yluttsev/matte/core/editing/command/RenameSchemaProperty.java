package io.github.yluttsev.matte.core.editing.command;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;

public record RenameSchemaProperty(
        ElementId schemaId,
        ElementId propertyId,
        String newName
) implements EditCommand {

    public RenameSchemaProperty {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(newName, "newName");
    }
}
