package io.github.yluttsev.matte.core.model.schema;

import java.util.Objects;

public record SchemaReference(String value) {

    public SchemaReference {
        Objects.requireNonNull(value, "value");
    }
}
