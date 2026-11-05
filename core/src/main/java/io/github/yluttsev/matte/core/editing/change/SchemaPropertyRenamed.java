package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

public record SchemaPropertyRenamed(
        ElementId schemaId,
        ElementId propertyId,
        DocumentPath schemaPath,
        String oldName,
        String newName
) implements Change {

    public SchemaPropertyRenamed {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(schemaPath, "schemaPath");
        Objects.requireNonNull(oldName, "oldName");
        Objects.requireNonNull(newName, "newName");
    }
}
