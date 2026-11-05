package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

public record SchemaPropertyDeleted(
        ElementId schemaId,
        ElementId propertyId,
        String name,
        DocumentPath path
) implements Change {

    public SchemaPropertyDeleted {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(path, "path");
    }
}
