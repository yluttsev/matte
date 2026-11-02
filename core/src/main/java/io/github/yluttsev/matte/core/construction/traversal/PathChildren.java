package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.model.path.Operation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import java.util.function.BiConsumer;

final class PathChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, String path, BiConsumer<Object, String> visitor) {
        if (node instanceof PathItem item) {
            visitor.accept(item.getOperations(), path + ".operations");
            visitor.accept(item.getParameters(), path + ".parameters");
            visitor.accept(item.getServers(), path + ".servers");
        } else if (node instanceof Operation operation) {
            visitor.accept(operation.getParameters(), path + ".parameters");
            visitor.accept(operation.getRequestBody(), path + ".requestBody");
            visitor.accept(operation.getResponses(), path + ".responses");
            visitor.accept(operation.getServers(), path + ".servers");
            visitor.accept(operation.getSecurity(), path + ".security");
        } else {
            return false;
        }
        return true;
    }
}
