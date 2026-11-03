package io.github.yluttsev.matte.openapi.patch;

import io.github.yluttsev.matte.core.navigation.DocumentPath;

record SourceScalar(DocumentPath path, boolean key, String value, int start, int end, QuoteStyle style) {

    enum QuoteStyle {
        JSON,
        PLAIN,
        SINGLE_QUOTED,
        DOUBLE_QUOTED
    }
}
