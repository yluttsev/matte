package io.github.yluttsev.matte.core.editing;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.model.ApiDocument;

public final class MutationAccess {

    private final Set<CopyableModel<?>> owned = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean active = true;

    MutationAccess(ApiDocument document) {
        new ModelWalker().walk(document, (node, path) -> owned.add(node));
    }

    public void require(CopyableModel<?> model) {
        if (!active || !owned.contains(model)) {
            throw new IllegalStateException("Model is not owned by an active editing session");
        }
    }

    void close() {
        active = false;
        owned.clear();
    }
}
