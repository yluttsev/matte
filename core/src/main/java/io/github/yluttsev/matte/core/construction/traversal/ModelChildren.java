package io.github.yluttsev.matte.core.construction.traversal;

import java.util.function.BiConsumer;

interface ModelChildren {

    boolean visitChildren(Object node, String path, BiConsumer<Object, String> visitor);
}
