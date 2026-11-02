package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.util.Objects;

public record ReferenceUsage<T extends CopyableModel<?>>(
        DocumentPath sourcePath,
        String reference,
        ReferenceKind kind,
        Class<T> expectedType,
        ReferenceResolution<T> resolution
) {

    public ReferenceUsage {
        Objects.requireNonNull(sourcePath, "sourcePath");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(expectedType, "expectedType");
        Objects.requireNonNull(resolution, "resolution");
    }
}
