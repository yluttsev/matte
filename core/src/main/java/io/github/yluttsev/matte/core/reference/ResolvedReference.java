package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.util.List;
import java.util.Objects;

public record ResolvedReference<T extends CopyableModel<?>>(
        T value,
        DocumentPath targetPath,
        List<DocumentPath> traversedPaths
) implements ReferenceResolution<T> {

    public ResolvedReference {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(targetPath, "targetPath");
        traversedPaths = List.copyOf(traversedPaths);
    }
}
