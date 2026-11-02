package io.github.yluttsev.matte.core.model;

import java.util.Objects;
import java.util.UUID;

public record ElementId(UUID value) {

    public ElementId {
        Objects.requireNonNull(value, "value");
    }
}
