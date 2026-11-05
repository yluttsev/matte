package io.github.yluttsev.matte.core.editing;

import io.github.yluttsev.matte.core.model.ApiDocument;

final class SchemaComponentNames {

    private SchemaComponentNames() {
    }

    static boolean hasSchemaComponentNamed(ApiDocument document, String name) {
        return document.getComponents().getSchemas().stream().anyMatch(component -> component.getName().equals(name));
    }
}
