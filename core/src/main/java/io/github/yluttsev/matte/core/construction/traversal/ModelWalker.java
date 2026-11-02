package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class ModelWalker {

    private static final List<ModelChildren> CHILDREN = List.of(
            new DocumentChildren(),
            new SchemaChildren(),
            new PathChildren(),
            new PayloadChildren(),
            new SecurityChildren());

    public void walk(CopyableModel<?> root, BiConsumer<CopyableModel<?>, String> visitor) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(visitor, "visitor");
        visit(root, "$", visitor, new IdentityHashMap<>());
    }

    private void visit(Object node, String path, BiConsumer<CopyableModel<?>, String> visitor, Map<Object, String> owners) {
        if (node == null) {
            return;
        }
        if (node instanceof Map<?, ?> entries) {
            entries.forEach((key, value) -> visit(value, path + "[" + key + "]", visitor, owners));
            return;
        }
        if (node instanceof Iterable<?> entries) {
            int index = 0;
            for (Object entry : entries) {
                visit(entry, path + "[" + index++ + "]", visitor, owners);
            }
            return;
        }
        if (!(node instanceof CopyableModel<?> model)) {
            return;
        }

        String owner = owners.putIfAbsent(model, path);
        if (owner != null) {
            throw new IllegalArgumentException("Repeated model ownership at " + path + "; first at " + owner);
        }

        visitor.accept(model, path);
        BiConsumer<Object, String> childVisitor =
                (child, childPath) -> visit(child, childPath, visitor, owners);
        for (ModelChildren children : CHILDREN) {
            if (children.visitChildren(model, path, childVisitor)) {
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported model type at " + path + ": " + model.getClass());
    }
}
