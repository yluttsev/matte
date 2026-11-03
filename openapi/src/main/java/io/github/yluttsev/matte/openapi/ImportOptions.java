package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;

public record ImportOptions(Optional<SourceFormat> formatHint) {

    public ImportOptions {
        Objects.requireNonNull(formatHint, "formatHint");
    }

    public static ImportOptions autoDetect() {
        return new ImportOptions(Optional.empty());
    }

    public static ImportOptions withFormat(SourceFormat format) {
        return new ImportOptions(Optional.of(format));
    }
}
