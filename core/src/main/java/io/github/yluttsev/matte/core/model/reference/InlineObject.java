package io.github.yluttsev.matte.core.model.reference;

import java.util.Objects;

public record InlineObject<T>(T value) implements ReferenceOr<T> {

    public InlineObject {
        Objects.requireNonNull(value, "value");
    }
}
