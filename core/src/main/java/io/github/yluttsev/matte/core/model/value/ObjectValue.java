package io.github.yluttsev.matte.core.model.value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ObjectValue(Map<String, DocumentValue> properties) implements DocumentValue {

    public ObjectValue {
        Map<String, DocumentValue> copy = new LinkedHashMap<>();
        properties.forEach((name, value) -> copy.put(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(value, "value")
        ));
        properties = Collections.unmodifiableMap(copy);
    }
}
