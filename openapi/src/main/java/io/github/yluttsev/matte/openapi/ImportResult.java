package io.github.yluttsev.matte.openapi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ImportResult(
        SourceDocument source,
        Optional<SourceFormat> format,
        ImportStatus status,
        List<ImportDiagnostic> diagnostics
) {

    public ImportResult {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(diagnostics);
    }
}
