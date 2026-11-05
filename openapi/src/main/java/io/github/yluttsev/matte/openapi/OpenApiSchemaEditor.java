package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;
import io.github.yluttsev.matte.core.editing.DocumentEditor;
import io.github.yluttsev.matte.core.editing.EditResult;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentAdded;
import io.github.yluttsev.matte.core.editing.change.SchemaObjectUpdated;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyAdded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.patch.OpenApiAdditionPatcher;
import io.github.yluttsev.matte.openapi.patch.OpenApiSourcePatcher;
import io.github.yluttsev.matte.openapi.patch.SchemaFieldPatcher;
import io.github.yluttsev.matte.openapi.patch.SchemaSourceVerifier;

public final class OpenApiSchemaEditor {

    public RenameResult rename(ImportResult imported, RenameSchemaComponent command) {
        OpenApiEditResult result = apply(imported, command);
        return new RenameResult(result.edit(), result.source(), result.state());
    }

    public OpenApiEditResult addProperty(ImportResult imported, AddSchemaProperty command) {
        return apply(imported, command);
    }

    public OpenApiEditResult addComponent(ImportResult imported, AddSchemaComponent command) {
        return apply(imported, command);
    }

    public OpenApiEditResult updateSchema(ImportResult imported, UpdateSchemaObject command) {
        return apply(imported, command);
    }

    private OpenApiEditResult apply(ImportResult imported, EditCommand command) {
        Objects.requireNonNull(imported, "imported");
        Objects.requireNonNull(command, "command");
        if (imported.status() != ImportStatus.IMPORTED) {
            throw new IllegalArgumentException("Only imported OpenAPI documents can be edited");
        }
        EditResult edit = new DocumentEditor().apply(imported.document().orElseThrow(), command);
        if (!(edit instanceof EditSucceeded succeeded)) {
            return new OpenApiEditResult(edit, Optional.empty(), imported);
        }
        SourceFormat format = imported.format().orElseThrow();
        OpenApiVersion version = OpenApiVersion.supported(imported.version().orElseThrow()).orElseThrow();
        SourceDocument source = sourceAfterEdit(imported, command, succeeded, format, version);
        ImportResult checked = new OpenApiImporter().importDocument(source.content(), ImportOptions.withFormat(format));
        if (checked.status() != ImportStatus.IMPORTED || !checked.version().equals(imported.version())) {
            throw new IllegalArgumentException("Patched OpenAPI document could not be imported");
        }
        if (command instanceof AddSchemaComponent || command instanceof AddSchemaProperty) {
            verifyAddition(imported, checked, succeeded.changeSet().changes().getFirst(), version);
        } else if (command instanceof UpdateSchemaObject) {
            verifyUpdate(checked, succeeded.changeSet().changes().getFirst(), version);
        }
        ImportResult state = new ImportResult(source, checked.format(), checked.status(), checked.diagnostics(),
                checked.version(), Optional.of(succeeded.document()));
        return new OpenApiEditResult(edit, Optional.of(source), state);
    }

    private SourceDocument sourceAfterEdit(
            ImportResult imported,
            EditCommand command,
            EditSucceeded succeeded,
            SourceFormat format,
            OpenApiVersion version
    ) {
        if (command instanceof RenameSchemaComponent) {
            return new OpenApiSourcePatcher().apply(imported.source(), format, succeeded.changeSet());
        }
        if (command instanceof UpdateSchemaObject) {
            SchemaObjectUpdated update = (SchemaObjectUpdated) succeeded.changeSet().changes().getFirst();
            return new SchemaFieldPatcher().apply(imported.source(), format, version, update);
        }
        return new OpenApiAdditionPatcher().apply(imported, succeeded.changeSet());
    }

    private void verifyAddition(ImportResult before, ImportResult after, Change change, OpenApiVersion version) {
        DocumentPath path;
        Schema expected;
        if (change instanceof SchemaComponentAdded component) {
            path = DocumentPath.schemaComponent(component.name());
            expected = component.schema();
        } else if (change instanceof SchemaPropertyAdded property) {
            DocumentPath schema = DocumentIndex.build(before.document().orElseThrow())
                    .findById(property.schemaId()).orElseThrow().path();
            path = schema.child("properties").child(property.name());
            expected = property.schema();
        } else {
            throw new IllegalArgumentException("Unsupported schema addition");
        }
        new SchemaSourceVerifier().verifyAddedSchema(after.source(), after.format().orElseThrow(), path,
                expected, version);
    }

    private void verifyUpdate(ImportResult imported, Change change, OpenApiVersion version) {
        if (!(change instanceof SchemaObjectUpdated update)) {
            throw new IllegalArgumentException("Unsupported schema update");
        }
        new SchemaSourceVerifier().verifyUpdatedSchema(imported.source(), imported.format().orElseThrow(),
                update.path(), update.before(), update.after(), version);
    }

    public record RenameResult(EditResult edit, Optional<SourceDocument> source, ImportResult state) {

        public RenameResult {
            Objects.requireNonNull(edit, "edit");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(state, "state");
        }
    }
}
