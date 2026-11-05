package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

public record SchemaComponentDeleted(
        ElementId componentId,
        String name,
        DocumentPath path
) implements Change {

    public SchemaComponentDeleted {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(path, "path");
    }
}
