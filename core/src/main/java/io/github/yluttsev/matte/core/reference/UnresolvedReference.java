package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.util.List;
import java.util.Objects;

public record UnresolvedReference<T extends CopyableModel<?>>(
        ReferenceFailure reason,
        List<DocumentPath> traversedPaths
) implements ReferenceResolution<T> {

    public UnresolvedReference {
        Objects.requireNonNull(reason, "reason");
        traversedPaths = List.copyOf(traversedPaths);
    }
}
