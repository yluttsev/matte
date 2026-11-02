package io.github.yluttsev.matte.core.model.schema;

import io.github.yluttsev.matte.core.model.ElementId;

public sealed interface Schema permits SchemaObject, BooleanSchema {

    ElementId getId();
}
