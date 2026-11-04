package io.github.yluttsev.matte.openapi.patch;

import java.util.Map;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

interface SourceAdditionWriter {

    String add(String text, DocumentPath parent, DocumentPath requiredAncestor, String name,
            Map<String, Object> schema);
}
