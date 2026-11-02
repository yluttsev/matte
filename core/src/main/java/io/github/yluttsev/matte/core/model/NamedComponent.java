package io.github.yluttsev.matte.core.model;

import java.util.Objects;

public final class NamedComponent<T> {

    private final ElementId id;
    private String name;
    private T value;

    public NamedComponent(ElementId id, String name, T value) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.value = Objects.requireNonNull(value, "value");
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
}
