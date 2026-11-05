package io.github.yluttsev.matte.core.editing.command;

import java.util.Objects;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;

public record UpdateSchemaObject(SchemaObject schema) implements EditCommand {

    public UpdateSchemaObject {
        schema = Objects.requireNonNull(schema, "schema").copy();
    }
}
