package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
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
        visit(root, new ModelLocation("$", DocumentPath.root()),
                (model, location) -> visitor.accept(model, location.diagnosticPath()), new IdentityHashMap<>());
    }

    public void walkDocument(ApiDocument document, BiConsumer<CopyableModel<?>, DocumentPath> visitor) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(visitor, "visitor");
        visit(document, new ModelLocation("$", DocumentPath.root()),
                (model, location) -> visitor.accept(model, location.documentPath()), new IdentityHashMap<>());
    }

    private void visit(
            Object node,
            ModelLocation location,
            BiConsumer<CopyableModel<?>, ModelLocation> visitor,
            Map<Object, String> owners
    ) {
        if (node == null) {
            return;
        }
        if (node instanceof Map<?, ?> entries) {
            entries.forEach((key, value) -> visit(value, location.mapEntry(key), visitor, owners));
            return;
        }
        if (node instanceof Iterable<?> entries) {
            int index = 0;
            for (Object entry : entries) {
                visit(entry, location.listEntry(entry, index++), visitor, owners);
            }
            return;
        }
        if (!(node instanceof CopyableModel<?> model)) {
            return;
        }

        String path = location.diagnosticPath();
        String owner = owners.putIfAbsent(model, path);
        if (owner != null) {
            throw new IllegalArgumentException("Repeated model ownership at " + path + "; first at " + owner);
        }

        visitor.accept(model, location);
        BiConsumer<Object, String> childVisitor =
                (child, field) -> visit(child, location.field(model, field), visitor, owners);
        for (ModelChildren children : CHILDREN) {
            if (children.visitChildren(model, childVisitor)) {
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported model type at " + path + ": " + model.getClass());
    }
}
