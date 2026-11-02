package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.util.List;

public sealed interface ReferenceResolution<T extends CopyableModel<?>>
        permits ResolvedReference, UnresolvedReference {

    List<DocumentPath> traversedPaths();
}
