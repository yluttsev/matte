package io.github.yluttsev.matte.core.editing;

import java.util.function.UnaryOperator;

public interface ReferenceOwner {

    void rewriteReferences(MutationAccess access, UnaryOperator<String> transform);
}
