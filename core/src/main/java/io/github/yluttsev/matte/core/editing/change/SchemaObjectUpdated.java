package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

public record SchemaObjectUpdated(
        ElementId schemaId,
        DocumentPath path,
        SchemaObject before,
        SchemaObject after
) implements Change {

    public SchemaObjectUpdated {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(path, "path");
        before = Objects.requireNonNull(before, "before").copy();
        after = Objects.requireNonNull(after, "after").copy();
    }
}
