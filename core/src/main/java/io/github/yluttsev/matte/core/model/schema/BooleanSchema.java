package io.github.yluttsev.matte.core.model.schema;

import io.github.yluttsev.matte.core.model.ElementId;

import java.util.Objects;

public final class BooleanSchema implements Schema {

    private final ElementId id;
    private boolean value;

    public BooleanSchema(ElementId id, boolean value) {
        this.id = Objects.requireNonNull(id, "id");
        this.value = value;
    }

    @Override
    public ElementId getId() {
        return id;
    }

    public boolean getValue() {
        return value;
    }
}
