package io.github.yluttsev.matte.core.model;

import java.util.Objects;
import java.util.function.UnaryOperator;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.editing.MutationAccess;
import io.github.yluttsev.matte.core.editing.ReferenceOwner;
import io.github.yluttsev.matte.core.editing.ReferenceUpdates;

public final class NamedComponent<T extends CopyableModel<T>>
        implements IdentifiedElement, CopyableModel<NamedComponent<T>>, ReferenceOwner {

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
    public void rewriteReferences(MutationAccess access, UnaryOperator<String> transform) {
        access.require(this);
        value = ReferenceUpdates.value(value, transform);
    }

    @Override
    public NamedComponent<T> copy() {
        return new NamedComponent<>(id, name, value);
    }
}
