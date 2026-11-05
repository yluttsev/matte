package io.github.yluttsev.matte.core.construction.traversal;

import java.util.Locale;
import io.github.yluttsev.matte.core.model.NamedComponent;
import io.github.yluttsev.matte.core.model.path.HttpMethod;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.reference.InlineObject;
import io.github.yluttsev.matte.core.model.response.ResponseKey;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

record ModelLocation(String diagnosticPath, DocumentPath documentPath) {

    ModelLocation field(Object owner, String field) {
        DocumentPath nextPath = documentPath;
        if (!omitsFieldInDocumentPath(owner, field)) {
            nextPath = documentPath.child(field);
        }
        return new ModelLocation(diagnosticPath + "." + field, nextPath);
    }

    ModelLocation mapEntry(Object key) {
        return new ModelLocation(diagnosticPath + "[" + key + "]", documentPath.child(mapKeySegment(key)));
    }

    ModelLocation listEntry(Object entry, int index) {
        return new ModelLocation(
                diagnosticPath + "[" + index + "]",
                documentPath.child(listEntrySegment(entry, index))
        );
    }

    private static boolean omitsFieldInDocumentPath(Object owner, String field) {
        return isTransparentContainer(owner) || isPathOperationsField(owner, field);
    }

    private static boolean isTransparentContainer(Object owner) {
        return owner instanceof SchemaComponent
                || owner instanceof SchemaProperty
                || owner instanceof NamedComponent<?>
                || owner instanceof InlineObject<?>;
    }

    private static boolean isPathOperationsField(Object owner, String field) {
        return owner instanceof PathItem && field.equals(PathChildren.OPERATIONS_FIELD);
    }

    private static String mapKeySegment(Object key) {
        if (key instanceof HttpMethod method) {
            return method.name().toLowerCase(Locale.ROOT);
        }
        if (key instanceof ResponseKey response) {
            return response.value();
        }
        return key.toString();
    }

    private static String listEntrySegment(Object entry, int index) {
        if (entry instanceof SchemaComponent component) {
            return component.getName();
        }
        if (entry instanceof NamedComponent<?> component) {
            return component.getName();
        }
        if (entry instanceof SchemaProperty property) {
            return property.getName();
        }
        if (entry instanceof PathItem path) {
            return path.getPath();
        }
        return Integer.toString(index);
    }
}
