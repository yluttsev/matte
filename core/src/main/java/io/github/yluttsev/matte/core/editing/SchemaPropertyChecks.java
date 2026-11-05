package io.github.yluttsev.matte.core.editing;

import java.util.ArrayList;
import java.util.List;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;

final class SchemaPropertyChecks {

    private SchemaPropertyChecks() {
    }

    static SchemaProperty findProperty(
            SchemaObject schema,
            ElementId propertyId
    ) {
        return schema.getProperties().stream()
                .filter(property -> property.getId().equals(propertyId))
                .findFirst()
                .orElse(null);
    }

    static boolean hasPropertyNamed(
            SchemaObject schema,
            String name
    ) {
        return schema.getProperties().stream().anyMatch(property -> property.getName().equals(name));
    }

    static boolean containsBlankPropertyName(CopyableModel<?> addition) {
        List<SchemaProperty> invalid = new ArrayList<>();
        new ModelWalker().walk(addition, (node, path) -> {
            if (node instanceof SchemaProperty property && property.getName().isBlank()) {
                invalid.add(property);
            }
        });
        return !invalid.isEmpty();
    }
}
