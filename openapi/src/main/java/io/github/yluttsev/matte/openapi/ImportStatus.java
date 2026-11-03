package io.github.yluttsev.matte.openapi;

public enum ImportStatus {
    PARSED,
    EMPTY_SOURCE,
    INVALID_ENCODING,
    AMBIGUOUS_FORMAT,
    SYNTAX_ERROR,
    DUPLICATE_KEY,
    MULTIPLE_DOCUMENTS
}
