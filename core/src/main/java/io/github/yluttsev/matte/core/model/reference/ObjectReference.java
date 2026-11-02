package io.github.yluttsev.matte.core.model.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;

import java.util.Objects;

public record ObjectReference<T extends CopyableModel<T>>(
        ElementId id,
        String value,
        String summary,
        String description
) implements IdentifiedElement, ReferenceOr<T> {

    public ObjectReference {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
    }

    @Override
    public ElementId getId() {
        return id;
    }

    @Override
    public ObjectReference<T> copy() {
        return new ObjectReference<>(id, value, summary, description);
    }
}
