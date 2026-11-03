package io.github.yluttsev.matte.core.editing.change;

import java.util.Objects;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceKind;

public record ReferenceChanged(
        DocumentPath sourceBefore,
        DocumentPath sourceAfter,
        String oldValue,
        String newValue,
        ReferenceKind kind
) implements Change {

    public ReferenceChanged {
        Objects.requireNonNull(sourceBefore, "sourceBefore");
        Objects.requireNonNull(sourceAfter, "sourceAfter");
        Objects.requireNonNull(oldValue, "oldValue");
        Objects.requireNonNull(newValue, "newValue");
        Objects.requireNonNull(kind, "kind");
    }
}
