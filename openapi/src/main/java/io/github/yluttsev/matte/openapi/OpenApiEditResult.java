package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

public sealed interface OpenApiEditResult {

    ImportResult state();

    default Optional<SourceDocument> source() {
        return Optional.empty();
    }

    record Applied(EditSucceeded edit, SourceDocument updatedSource, ImportResult state) implements OpenApiEditResult {

        public Applied {
            Objects.requireNonNull(edit, "edit");
            Objects.requireNonNull(updatedSource, "updatedSource");
            Objects.requireNonNull(state, "state");
        }

        @Override
        public Optional<SourceDocument> source() {
            return Optional.of(updatedSource);
        }
    }

    record Rejected(EditRejected edit, ImportResult state) implements OpenApiEditResult {

        public Rejected {
            Objects.requireNonNull(edit, "edit");
            Objects.requireNonNull(state, "state");
        }
    }

    record Refused(
            SourceEditCode code,
            String message,
            DocumentPath path,
            ImportResult state
    ) implements OpenApiEditResult {

        public Refused {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(state, "state");
        }
    }
}
