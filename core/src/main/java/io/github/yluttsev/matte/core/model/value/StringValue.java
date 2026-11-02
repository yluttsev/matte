package io.github.yluttsev.matte.core.model.value;

import java.util.Objects;

public record StringValue(String value) implements DocumentValue {

    public StringValue {
        Objects.requireNonNull(value, "value");
    }
}
