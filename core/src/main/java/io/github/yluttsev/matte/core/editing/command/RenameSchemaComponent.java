package io.github.yluttsev.matte.core.editing.command;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;

public record RenameSchemaComponent(ElementId componentId, String newName) implements EditCommand {

    public RenameSchemaComponent {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(newName, "newName");
    }
}
