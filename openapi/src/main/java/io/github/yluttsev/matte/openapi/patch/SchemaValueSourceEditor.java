package io.github.yluttsev.matte.openapi.patch;

import io.github.yluttsev.matte.core.navigation.DocumentPath;

interface SchemaValueSourceEditor {

    String replace(String text, DocumentPath path, Object value);

    String remove(String text, DocumentPath path);
}
