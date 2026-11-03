package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;

public record ImportDiagnostic(ImportStatus code, String message, Optional<SourceLocation> location) {

    private static final String EMPTY_SOURCE_MESSAGE = "Source document is empty";

    public ImportDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(location, "location");
    }

    public ImportDiagnostic(ImportStatus code, String message) {
        this(code, message, Optional.empty());
    }

    public static ImportDiagnostic emptySource() {
        return new ImportDiagnostic(ImportStatus.EMPTY_SOURCE, EMPTY_SOURCE_MESSAGE);
    }
}
