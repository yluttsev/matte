package io.github.yluttsev.matte.openapi.patch;

import io.github.yluttsev.matte.core.navigation.DocumentPath;

interface SourceAdditionWriter {

    String add(
            String text,
            DocumentPath parent,
            DocumentPath requiredAncestor,
            String name,
            Object value
    );
}
