package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;

public record ImportOptions(Optional<SourceFormat> formatHint, int maxSourceBytes) {

    public static final int MAX_SUPPORTED_SOURCE_BYTES = 3 * 1024 * 1024;
    public static final int DEFAULT_MAX_SOURCE_BYTES = MAX_SUPPORTED_SOURCE_BYTES;

    public ImportOptions(Optional<SourceFormat> formatHint) {
        this(formatHint, DEFAULT_MAX_SOURCE_BYTES);
    }

    public ImportOptions {
        Objects.requireNonNull(formatHint, "formatHint");
        if (maxSourceBytes <= 0 || maxSourceBytes > MAX_SUPPORTED_SOURCE_BYTES) {
            throw new IllegalArgumentException("maxSourceBytes must be between 1 and " + MAX_SUPPORTED_SOURCE_BYTES);
        }
    }

    public static ImportOptions autoDetect() {
        return new ImportOptions(Optional.empty());
    }

    public static ImportOptions withFormat(SourceFormat format) {
        return new ImportOptions(Optional.of(format));
    }

    public ImportOptions withMaxSourceBytes(int limit) {
        return new ImportOptions(formatHint, limit);
    }
}
