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
        boolean transparent = owner instanceof SchemaComponent || owner instanceof SchemaProperty
                || owner instanceof NamedComponent<?> || owner instanceof InlineObject<?>
                || owner instanceof PathItem && field.equals("operations");
        return new ModelLocation(diagnosticPath + "." + field,
                transparent ? documentPath : documentPath.child(field));
    }

    ModelLocation mapEntry(Object key) {
        String segment;
        if (key instanceof HttpMethod method) {
            segment = method.name().toLowerCase(Locale.ROOT);
        } else if (key instanceof ResponseKey response) {
            segment = response.value();
        } else {
            segment = key.toString();
        }
        return new ModelLocation(diagnosticPath + "[" + key + "]", documentPath.child(segment));
    }

    ModelLocation listEntry(Object entry, int index) {
        String segment;
        if (entry instanceof SchemaComponent component) {
            segment = component.getName();
        } else if (entry instanceof NamedComponent<?> component) {
            segment = component.getName();
        } else if (entry instanceof SchemaProperty property) {
            segment = property.getName();
        } else if (entry instanceof PathItem path) {
            segment = path.getPath();
        } else {
            segment = Integer.toString(index);
        }
        return new ModelLocation(diagnosticPath + "[" + index + "]", documentPath.child(segment));
    }
}
