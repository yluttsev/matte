package io.github.yluttsev.matte.openapi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import io.github.yluttsev.matte.core.editing.DocumentEditor;
import io.github.yluttsev.matte.core.editing.EditCode;
import io.github.yluttsev.matte.core.editing.EditIssue;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.EditResult;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentAdded;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentDeleted;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentRenamed;
import io.github.yluttsev.matte.core.editing.change.SchemaObjectUpdated;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyAdded;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyDeleted;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyRenamed;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.patch.OpenApiAdditionPatcher;
import io.github.yluttsev.matte.openapi.patch.OpenApiSchemaDeletionPatcher;
import io.github.yluttsev.matte.openapi.patch.OpenApiSourcePatcher;
import io.github.yluttsev.matte.openapi.patch.SchemaFieldPatcher;
import io.github.yluttsev.matte.openapi.patch.SchemaSourceVerifier;

public final class OpenApiSchemaEditor {

    private static final String PROPERTIES_FIELD = "properties";

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

    public OpenApiEditResult renameProperty(ImportResult imported, RenameSchemaProperty command) {
        return apply(imported, command);
    }

    public OpenApiEditResult deleteProperty(ImportResult imported, DeleteSchemaProperty command) {
        return apply(imported, command);
    }

    public OpenApiEditResult deleteComponent(ImportResult imported, DeleteSchemaComponent command) {
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
        Optional<OpenApiEditResult> sourceReferenceRejection = rejectUnmappedIncomingReferences(
                imported,
                succeeded,
                format
        );
        if (sourceReferenceRejection.isPresent()) {
            return sourceReferenceRejection.orElseThrow();
        }
        SourceDocument source = sourceAfterEdit(imported, command, succeeded, format, version);
        ImportResult checked = new OpenApiImporter().importDocument(source.content(), ImportOptions.withFormat(format));
        if (checked.status() != ImportStatus.IMPORTED || !checked.version().equals(imported.version())) {
            throw new IllegalArgumentException(
                    "Patched OpenAPI document could not be imported: " + checked.diagnostics()
            );
        }
        if (!succeeded.changeSet().changes().isEmpty()) {
            Change change = succeeded.changeSet().changes().getFirst();
            if (command instanceof AddSchemaComponent || command instanceof AddSchemaProperty) {
                verifyAddition(imported, checked, change, version);
            } else if (command instanceof UpdateSchemaObject) {
                verifyUpdate(checked, change, version);
            } else {
                verifyStructuralEdit(checked, change);
            }
        }
        ImportResult state = new ImportResult(
                source,
                checked.format(),
                checked.status(),
                checked.diagnostics(),
                checked.version(),
                Optional.of(succeeded.document())
        );
        return new OpenApiEditResult(edit, Optional.of(source), state);
    }

    private SourceDocument sourceAfterEdit(
            ImportResult imported,
            EditCommand command,
            EditSucceeded succeeded,
            SourceFormat format,
            OpenApiVersion version
    ) {
        if (command instanceof RenameSchemaComponent || command instanceof RenameSchemaProperty) {
            return new OpenApiSourcePatcher().apply(imported.source(), format, succeeded.changeSet());
        }
        if (command instanceof DeleteSchemaComponent || command instanceof DeleteSchemaProperty) {
            Change change = succeeded.changeSet().changes().getFirst();
            DocumentPath path = deletionPath(change);
            return new OpenApiSchemaDeletionPatcher().remove(imported.source(), format, path);
        }
        if (command instanceof UpdateSchemaObject) {
            SchemaObjectUpdated update = (SchemaObjectUpdated) succeeded.changeSet().changes().getFirst();
            return new SchemaFieldPatcher().apply(imported.source(), format, version, update);
        }
        return new OpenApiAdditionPatcher().apply(imported, succeeded.changeSet());
    }

    private Optional<OpenApiEditResult> rejectUnmappedIncomingReferences(
            ImportResult imported,
            EditSucceeded succeeded,
            SourceFormat format
    ) {
        if (succeeded.changeSet().changes().isEmpty()) {
            return Optional.empty();
        }
        if (!isDeletion(succeeded.changeSet().changes().getFirst())) {
            return Optional.empty();
        }
        Change change = succeeded.changeSet().changes().getFirst();
        DocumentPath targetPath = deletionPath(change);
        List<DocumentPath> references = new OpenApiSchemaDeletionPatcher().incomingReferences(
                imported.source(),
                format,
                targetPath
        );
        if (references.isEmpty()) {
            return Optional.empty();
        }
        String locations = references.stream().map(Object::toString).collect(Collectors.joining(", "));
        String target = deletionDescription(change);
        EditRejected rejection = new EditRejected(List.of(new EditIssue(
                EditCode.REFERENCED_TARGET,
                "Cannot delete " + target + "; incoming references: " + locations
        )));
        return Optional.of(new OpenApiEditResult(rejection, Optional.empty(), imported));
    }

    private String deletionDescription(Change change) {
        if (change instanceof SchemaComponentDeleted component) {
            return "schema component '" + component.name() + "'";
        }
        if (change instanceof SchemaPropertyDeleted property) {
            return "schema property '" + property.name() + "'";
        }
        throw new IllegalArgumentException("Unsupported schema deletion");
    }

    private boolean isDeletion(Change change) {
        return change instanceof SchemaComponentDeleted || change instanceof SchemaPropertyDeleted;
    }

    private DocumentPath deletionPath(Change change) {
        if (change instanceof SchemaComponentDeleted component) {
            return component.path();
        }
        if (change instanceof SchemaPropertyDeleted property) {
            return property.path();
        }
        throw new IllegalArgumentException("Unsupported schema deletion");
    }

    private void verifyStructuralEdit(ImportResult imported, Change change) {
        DocumentIndex index = DocumentIndex.build(imported.document().orElseThrow());
        if (change instanceof SchemaPropertyRenamed property) {
            DocumentPath oldPath = property.schemaPath().child(PROPERTIES_FIELD).child(property.oldName());
            DocumentPath newPath = property.schemaPath().child(PROPERTIES_FIELD).child(property.newName());
            if (index.findByPath(oldPath).isPresent() || index.findByPath(newPath).isEmpty()) {
                throw new IllegalArgumentException("Renamed schema property was not preserved in source");
            }
            return;
        }
        if (isDeletion(change)) {
            if (index.findByPath(deletionPath(change)).isPresent()) {
                throw new IllegalArgumentException("Deleted schema element was not removed from source");
            }
            return;
        }
        if (change instanceof SchemaComponentRenamed component) {
            DocumentPath oldPath = DocumentPath.schemaComponent(component.oldName());
            DocumentPath newPath = DocumentPath.schemaComponent(component.newName());
            if (index.findByPath(oldPath).isPresent() || index.findByPath(newPath).isEmpty()) {
                throw new IllegalArgumentException("Renamed schema component was not preserved in source");
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported structural schema change");
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
            path = schema.child(PROPERTIES_FIELD).child(property.name());
            expected = property.schema();
        } else {
            throw new IllegalArgumentException("Unsupported schema addition");
        }
        new SchemaSourceVerifier().verifyAddedSchema(
                after.source(),
                after.format().orElseThrow(),
                path,
                expected,
                version
        );
    }

    private void verifyUpdate(ImportResult imported, Change change, OpenApiVersion version) {
        if (!(change instanceof SchemaObjectUpdated update)) {
            throw new IllegalArgumentException("Unsupported schema update");
        }
        new SchemaSourceVerifier().verifyUpdatedSchema(
                imported.source(),
                imported.format().orElseThrow(),
                update.path(),
                update.before(),
                update.after(),
                version
        );
    }

    public record RenameResult(EditResult edit, Optional<SourceDocument> source, ImportResult state) {

        public RenameResult {
            Objects.requireNonNull(edit, "edit");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(state, "state");
        }
    }
}
