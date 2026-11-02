package io.github.yluttsev.matte.core.model.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;

import java.util.Objects;

public record InlineObject<T extends CopyableModel<T>>(T value) implements ReferenceOr<T> {

    public InlineObject {
        value = Objects.requireNonNull(value, "value").copy();
    }

    @Override
    public InlineObject<T> copy() {
        return new InlineObject<>(value);
    }
}
