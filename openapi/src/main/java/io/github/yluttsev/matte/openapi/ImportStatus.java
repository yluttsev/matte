package io.github.yluttsev.matte.openapi;

public enum ImportStatus {
    EMPTY_SOURCE,
    INVALID_ENCODING,
    AMBIGUOUS_FORMAT,
    SYNTAX_ERROR,
    DUPLICATE_KEY,
    MULTIPLE_DOCUMENTS,
    IMPORTED,
    UNSUPPORTED_VERSION,
    INVALID_OPENAPI,
    MAPPING_ERROR
}
