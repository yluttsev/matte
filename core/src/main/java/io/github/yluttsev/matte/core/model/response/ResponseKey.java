package io.github.yluttsev.matte.core.model.response;

import java.util.Objects;

public record ResponseKey(String value) {

    public ResponseKey {
        Objects.requireNonNull(value, "value");
    }
}
