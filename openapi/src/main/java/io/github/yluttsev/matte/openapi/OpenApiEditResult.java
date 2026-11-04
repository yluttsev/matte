package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;
import io.github.yluttsev.matte.core.editing.EditResult;

public record OpenApiEditResult(EditResult edit, Optional<SourceDocument> source, ImportResult state) {

    public OpenApiEditResult {
        Objects.requireNonNull(edit, "edit");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(state, "state");
    }
}
