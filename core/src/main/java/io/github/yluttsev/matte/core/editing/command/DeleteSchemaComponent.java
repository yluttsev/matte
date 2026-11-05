package io.github.yluttsev.matte.core.editing.command;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.ElementId;

public record DeleteSchemaComponent(ElementId componentId) implements EditCommand {

    public DeleteSchemaComponent {
        Objects.requireNonNull(componentId, "componentId");
    }
}
