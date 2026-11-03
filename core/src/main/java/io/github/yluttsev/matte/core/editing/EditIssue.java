package io.github.yluttsev.matte.core.editing;

import java.util.Objects;

public record EditIssue(EditCode code, String message) {

    public EditIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
