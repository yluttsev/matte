package io.github.yluttsev.matte.core.model.value;

import java.math.BigDecimal;
import java.util.Objects;

public record NumberValue(BigDecimal value) implements DocumentValue {

    public NumberValue {
        Objects.requireNonNull(value, "value");
    }
}
