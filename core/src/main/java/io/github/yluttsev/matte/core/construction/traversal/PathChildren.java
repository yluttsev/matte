package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.model.path.Operation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import java.util.function.BiConsumer;

final class PathChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, BiConsumer<Object, String> visitor) {
        if (node instanceof PathItem item) {
            visitor.accept(item.getOperations(), "operations");
            visitor.accept(item.getParameters(), "parameters");
            visitor.accept(item.getServers(), "servers");
        } else if (node instanceof Operation operation) {
            visitor.accept(operation.getParameters(), "parameters");
            visitor.accept(operation.getRequestBody(), "requestBody");
            visitor.accept(operation.getResponses(), "responses");
            visitor.accept(operation.getServers(), "servers");
            visitor.accept(operation.getSecurity(), "security");
        } else {
            return false;
        }
        return true;
    }
}
