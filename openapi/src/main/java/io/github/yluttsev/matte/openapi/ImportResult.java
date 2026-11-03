package io.github.yluttsev.matte.openapi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import io.github.yluttsev.matte.core.model.ApiDocument;

public record ImportResult(
        SourceDocument source,
        Optional<SourceFormat> format,
        ImportStatus status,
        List<ImportDiagnostic> diagnostics,
        Optional<String> version,
        Optional<ApiDocument> document
) {

    public ImportResult {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(diagnostics);
        Objects.requireNonNull(version, "version");
        document = Objects.requireNonNull(document, "document").map(ApiDocument::copy);
    }

    public ImportResult(
            SourceDocument source,
            Optional<SourceFormat> format,
            ImportStatus status,
            List<ImportDiagnostic> diagnostics
    ) {
        this(source, format, status, diagnostics, Optional.empty(), Optional.empty());
    }

    @Override
    public Optional<ApiDocument> document() {
        return document.map(ApiDocument::copy);
    }
}
