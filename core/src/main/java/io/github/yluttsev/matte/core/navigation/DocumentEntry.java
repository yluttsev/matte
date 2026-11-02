package io.github.yluttsev.matte.core.navigation;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import java.util.Objects;

public record DocumentEntry(DocumentPath path, CopyableModel<?> value) {

    public DocumentEntry {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
    }
}
