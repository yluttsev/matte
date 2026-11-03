package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;

public record SchemaComponentRenamed(
        ElementId componentId,
        String oldName,
        String newName
) implements Change {

    public SchemaComponentRenamed {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(oldName, "oldName");
        Objects.requireNonNull(newName, "newName");
    }
}
