package io.github.yluttsev.matte.openapi.patch;

import java.util.Objects;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceEditCode;

public final class SourceEditRefusal extends RuntimeException {

    private final SourceEditCode code;
    private final DocumentPath path;

    SourceEditRefusal(SourceEditCode code, String message) {
        this(code, message, DocumentPath.root());
    }

    SourceEditRefusal(SourceEditCode code, String message, DocumentPath path) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.path = Objects.requireNonNull(path, "path");
    }

    public SourceEditCode code() {
        return code;
    }

    public DocumentPath path() {
        return path;
    }
}
