package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.NamedComponent;
import io.github.yluttsev.matte.core.model.info.ApiInfo;
import io.github.yluttsev.matte.core.model.reference.InlineObject;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.server.Server;
import io.github.yluttsev.matte.core.model.server.ServerVariable;
import io.github.yluttsev.matte.core.model.tag.Tag;
import java.util.function.BiConsumer;

final class DocumentChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, String path, BiConsumer<Object, String> visitor) {
        if (node instanceof ApiDocument document) {
            visitor.accept(document.getInfo(), path + ".info");
            visitor.accept(document.getServers(), path + ".servers");
            visitor.accept(document.getPaths(), path + ".paths");
            visitor.accept(document.getComponents(), path + ".components");
            visitor.accept(document.getTags(), path + ".tags");
            visitor.accept(document.getSecurity(), path + ".security");
        } else if (node instanceof Components components) {
            visitor.accept(components.getSchemas(), path + ".schemas");
            visitor.accept(components.getParameters(), path + ".parameters");
            visitor.accept(components.getRequestBodies(), path + ".requestBodies");
            visitor.accept(components.getResponses(), path + ".responses");
            visitor.accept(components.getHeaders(), path + ".headers");
            visitor.accept(components.getSecuritySchemes(), path + ".securitySchemes");
            visitor.accept(components.getExamples(), path + ".examples");
        } else if (node instanceof NamedComponent<?> component) {
            visitor.accept(component.getValue(), path + ".value");
        } else if (node instanceof InlineObject<?> inline) {
            visitor.accept(inline.value(), path + ".value");
        } else if (node instanceof Server server) {
            visitor.accept(server.getVariables(), path + ".variables");
        } else {
            return node instanceof ApiInfo || node instanceof ObjectReference<?>
                    || node instanceof ServerVariable || node instanceof Tag;
        }
        return true;
    }
}
