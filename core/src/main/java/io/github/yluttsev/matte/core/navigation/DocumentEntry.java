package io.github.yluttsev.matte.core.navigation;

import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;

public record DocumentEntry(DocumentPath path, CopyableModel<?> value) {

    public DocumentEntry {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
    }
}
