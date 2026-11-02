package io.github.yluttsev.matte.core.model.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;

public sealed interface ReferenceOr<T extends CopyableModel<T>>
        extends CopyableModel<ReferenceOr<T>> permits InlineObject, ObjectReference {
}
