package io.github.yluttsev.matte.openapi;

import java.util.List;
import java.util.Objects;

public final class OpenApiImporter {

    public ImportResult importDocument(byte[] content, ImportOptions options) {
        Objects.requireNonNull(options, "options");
        SourceDocument source = new SourceDocument(content);
        if (source.length() == 0) {
            return new ImportResult(source, options.formatHint(), ImportStatus.EMPTY_SOURCE,
                    List.of(new ImportDiagnostic(ImportStatus.EMPTY_SOURCE, "Source document is empty")));
        }
        return new ImportResult(source, options.formatHint(), ImportStatus.NOT_PARSED, List.of());
    }
}
