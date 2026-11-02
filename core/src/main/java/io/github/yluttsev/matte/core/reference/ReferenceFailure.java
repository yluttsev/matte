package io.github.yluttsev.matte.core.reference;

public enum ReferenceFailure {
    INVALID_REFERENCE,
    EXTERNAL_REFERENCE,
    UNSUPPORTED_ANCHOR,
    TARGET_NOT_FOUND,
    TYPE_MISMATCH,
    CYCLIC_REFERENCE
}
