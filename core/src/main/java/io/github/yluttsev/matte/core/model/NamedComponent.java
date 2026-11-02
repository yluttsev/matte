package io.github.yluttsev.matte.core.model;

import io.github.yluttsev.matte.core.construction.CopyableModel;

import java.util.Objects;

public final class NamedComponent<T extends CopyableModel<T>>
        implements IdentifiedElement, CopyableModel<NamedComponent<T>> {

    private final ElementId id;
    private String name;
    private T value;

    public NamedComponent(ElementId id, String name, T value) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.value = Objects.requireNonNull(value, "value").copy();
    }

    public ElementId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    @Override
    public NamedComponent<T> copy() {
        return new NamedComponent<>(id, name, value);
    }
}
