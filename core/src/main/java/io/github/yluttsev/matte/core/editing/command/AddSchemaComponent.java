package io.github.yluttsev.matte.core.editing.command;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;

public record AddSchemaComponent(SchemaComponent component) implements EditCommand {

    public AddSchemaComponent {
        component = Objects.requireNonNull(component, "component").copy();
    }
}
