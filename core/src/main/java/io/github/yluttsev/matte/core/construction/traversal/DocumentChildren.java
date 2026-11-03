package io.github.yluttsev.matte.core.construction.traversal;

import java.util.function.BiConsumer;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.NamedComponent;
import io.github.yluttsev.matte.core.model.info.ApiInfo;
import io.github.yluttsev.matte.core.model.reference.InlineObject;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.server.Server;
import io.github.yluttsev.matte.core.model.server.ServerVariable;
import io.github.yluttsev.matte.core.model.tag.Tag;

final class DocumentChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, BiConsumer<Object, String> visitor) {
        if (node instanceof ApiDocument document) {
            visitor.accept(document.getInfo(), "info");
            visitor.accept(document.getServers(), "servers");
            visitor.accept(document.getPaths(), "paths");
            visitor.accept(document.getComponents(), "components");
            visitor.accept(document.getTags(), "tags");
            visitor.accept(document.getSecurity(), "security");
        } else if (node instanceof Components components) {
            visitor.accept(components.getSchemas(), "schemas");
            visitor.accept(components.getParameters(), "parameters");
            visitor.accept(components.getRequestBodies(), "requestBodies");
            visitor.accept(components.getResponses(), "responses");
            visitor.accept(components.getHeaders(), "headers");
            visitor.accept(components.getSecuritySchemes(), "securitySchemes");
            visitor.accept(components.getExamples(), "examples");
        } else if (node instanceof NamedComponent<?> component) {
            visitor.accept(component.getValue(), "value");
        } else if (node instanceof InlineObject<?> inline) {
            visitor.accept(inline.value(), "value");
        } else if (node instanceof Server server) {
            visitor.accept(server.getVariables(), "variables");
        } else {
            return isLeafModel(node);
        }
        return true;
    }

    private static boolean isLeafModel(Object node) {
        return node instanceof ApiInfo
                || node instanceof ObjectReference<?>
                || node instanceof ServerVariable
                || node instanceof Tag;
    }
}
