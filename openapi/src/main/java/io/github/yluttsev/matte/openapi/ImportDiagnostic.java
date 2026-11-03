package io.github.yluttsev.matte.openapi;

import java.util.Objects;

public record ImportDiagnostic(ImportStatus code, String message) {

    public ImportDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
