package io.github.yluttsev.matte.core.construction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ModelCopies {

    private ModelCopies() {
    }

    public static <T extends CopyableModel<T>> T copy(T value) {
        if (value == null) {
            return null;
        }
        return value.copy();
    }

    public static <T extends CopyableModel<T>> List<T> list(Collection<? extends T> values) {
        List<T> result = new ArrayList<>();
        for (T value : values) {
            result.add(Objects.requireNonNull(value, "collection element").copy());
        }
        return result;
    }

    public static <K, T extends CopyableModel<T>> Map<K, T> copyModelMap(Map<K, T> values) {
        Map<K, T> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(
                Objects.requireNonNull(key, "map key"),
                Objects.requireNonNull(value, "map value").copy()
        ));
        return result;
    }

    public static <K, V> Map<K, V> copyMapEntries(Map<K, V> values) {
        Map<K, V> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(
                Objects.requireNonNull(key, "map key"),
                Objects.requireNonNull(value, "map value")
        ));
        return result;
    }
}
