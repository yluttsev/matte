package io.github.yluttsev.matte.core.model.schema;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.IdentifiedElement;

public sealed interface Schema extends IdentifiedElement, CopyableModel<Schema>
        permits SchemaObject, BooleanSchema {
}
