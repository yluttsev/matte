package io.github.yluttsev.matte.core.editing;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;

public final class ReferenceUpdates {

    private ReferenceUpdates() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends CopyableModel<T>> T value(T value, UnaryOperator<String> transform) {
        if (!(value instanceof ObjectReference<?> reference)) {
            return value;
        }
        String updated = Objects.requireNonNull(transform.apply(reference.value()), "updated reference");
        return updated.equals(reference.value()) ? value : (T) new ObjectReference<>(
                reference.id(), updated, reference.summary(), reference.description());
    }

    public static <T extends CopyableModel<T>> void list(List<T> values, UnaryOperator<String> transform) {
        values.replaceAll(value -> value(value, transform));
    }

    public static <K, T extends CopyableModel<T>> void map(Map<K, T> values, UnaryOperator<String> transform) {
        values.replaceAll((key, value) -> value(value, transform));
    }
}
