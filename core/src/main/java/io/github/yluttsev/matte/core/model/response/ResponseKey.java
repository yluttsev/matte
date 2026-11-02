package io.github.yluttsev.matte.core.model.response;

import java.util.Objects;

/** A response key, such as 200, 2XX or default; validity is checked separately. */
public record ResponseKey(String value) {

    public ResponseKey {
        Objects.requireNonNull(value, "value");
    }
}
