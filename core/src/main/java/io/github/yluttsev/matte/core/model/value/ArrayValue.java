package io.github.yluttsev.matte.core.model.value;

import java.util.List;

public record ArrayValue(List<DocumentValue> values) implements DocumentValue {

    public ArrayValue {
        values = List.copyOf(values);
    }
}
