package io.github.yluttsev.matte.core.construction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.NamedComponent;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;

public final class DocumentStructureValidator {

    private final Map<ElementId, String> ids = new HashMap<>();

    private DocumentStructureValidator() {
    }

    public static void validate(ApiDocument document) {
        DocumentStructureValidator validator = new DocumentStructureValidator();
        new ModelWalker().walk(Objects.requireNonNull(document, "document"), validator::validateNode);
    }

    private void validateNode(CopyableModel<?> node, String path) {
        validateId(node, path);
        validateNames(node, path);
    }

    private void validateId(CopyableModel<?> node, String path) {
        if (!(node instanceof IdentifiedElement element)) {
            return;
        }
        ElementId id = element.getId();
        String previous = ids.putIfAbsent(id, path);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate element ID " + id.value()
                    + " at " + path + "; first at " + previous);
        }
    }

    private void validateNames(CopyableModel<?> node, String path) {
        if (node instanceof ApiDocument document) {
            uniqueNames(document.getPaths(), PathItem::getPath, path + ".paths");
        } else if (node instanceof Components components) {
            uniqueNames(components.getSchemas(), SchemaComponent::getName, path + ".schemas");
            uniqueNames(components.getParameters(), NamedComponent::getName, path + ".parameters");
            uniqueNames(components.getRequestBodies(), NamedComponent::getName, path + ".requestBodies");
            uniqueNames(components.getResponses(), NamedComponent::getName, path + ".responses");
            uniqueNames(components.getHeaders(), NamedComponent::getName, path + ".headers");
            uniqueNames(components.getSecuritySchemes(), NamedComponent::getName, path + ".securitySchemes");
            uniqueNames(components.getExamples(), NamedComponent::getName, path + ".examples");
        } else if (node instanceof SchemaObject schema) {
            uniqueNames(schema.getProperties(), SchemaProperty::getName, path + ".properties");
        }
    }

    private <T> void uniqueNames(List<T> entries, Function<T, String> nameOf, String path) {
        Map<String, Integer> names = new HashMap<>();
        for (int index = 0; index < entries.size(); index++) {
            String name = nameOf.apply(entries.get(index));
            Integer previous = names.putIfAbsent(name, index);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate name '" + name + "' at " + path
                        + "[" + index + "]; first at " + path + "[" + previous + "]");
            }
        }
    }
}
