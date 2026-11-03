package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;
import io.github.yluttsev.matte.core.editing.DocumentEditor;
import io.github.yluttsev.matte.core.editing.EditResult;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.openapi.patch.OpenApiSourcePatcher;

public final class OpenApiSchemaEditor {

    public RenameResult rename(ImportResult imported, RenameSchemaComponent command) {
        Objects.requireNonNull(imported, "imported");
        Objects.requireNonNull(command, "command");
        if (imported.status() != ImportStatus.IMPORTED) {
            throw new IllegalArgumentException("Only imported OpenAPI documents can be edited");
        }
        EditResult edit = new DocumentEditor().apply(imported.document().orElseThrow(), command);
        if (!(edit instanceof EditSucceeded succeeded)) {
            return new RenameResult(edit, Optional.empty());
        }
        SourceFormat format = imported.format().orElseThrow();
        SourceDocument source = new OpenApiSourcePatcher().apply(imported.source(), format, succeeded.changeSet());
        ImportResult checked = new OpenApiImporter().importDocument(source.content(), ImportOptions.withFormat(format));
        if (checked.status() != ImportStatus.IMPORTED) {
            throw new IllegalArgumentException("Patched OpenAPI document could not be imported");
        }
        return new RenameResult(edit, Optional.of(source));
    }

    public record RenameResult(EditResult edit, Optional<SourceDocument> source) {

        public RenameResult {
            Objects.requireNonNull(edit, "edit");
            Objects.requireNonNull(source, "source");
        }
    }
}
