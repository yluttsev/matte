package io.github.yluttsev.matte.core.reference;

import java.util.List;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

public sealed interface ReferenceResolution<T extends CopyableModel<?>>
        permits ResolvedReference, UnresolvedReference {

    List<DocumentPath> traversedPaths();
}
