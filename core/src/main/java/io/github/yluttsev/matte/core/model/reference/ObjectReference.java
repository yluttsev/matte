package io.github.yluttsev.matte.core.model.reference;

import io.github.yluttsev.matte.core.model.ElementId;

import java.util.Objects;

public record ObjectReference<T>(ElementId id, String value, String summary, String description)
        implements ReferenceOr<T> {

    public ObjectReference {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
    }
}
