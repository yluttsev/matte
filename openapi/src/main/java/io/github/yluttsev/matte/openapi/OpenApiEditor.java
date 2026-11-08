package io.github.yluttsev.matte.openapi;

import java.util.List;
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
import io.github.yluttsev.matte.openapi.patch.SchemaIdScopeDetector;
import io.github.yluttsev.matte.openapi.patch.SchemaSourceVerifier;
import io.github.yluttsev.matte.openapi.patch.SourceEditRefusal;

public final class OpenApiEditor {

    private static final String PROPERTIES_FIELD = "properties";

    private final DocumentEditor documentEditor;
    private final OpenApiImporter importer;

    public OpenApiEditor() {
        this(new DocumentEditor(), new OpenApiImporter());
    }

    public OpenApiEditor(DocumentEditor documentEditor, OpenApiImporter importer) {
        if (documentEditor == null || importer == null) {
            throw new IllegalArgumentException("Editor and importer are required");
        }
        this.documentEditor = documentEditor;
        this.importer = importer;
    }

    public OpenApiEditResult rename(ImportResult imported, RenameSchemaComponent command) {
        return apply(imported, command);
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

    public OpenApiEditResult apply(ImportResult imported, EditCommand command) {
        if (imported == null || command == null) {
            throw new IllegalArgumentException("Imported state and command are required");
        }
        try {
            return applyCommand(imported, command);
        } catch (SourceEditRefusal refusal) {
            DocumentPath path = refusal.path();
            if (path.equals(DocumentPath.root())) {
                path = commandPath(imported, command);
            }
            return new OpenApiEditResult.Refused(refusal.code(), refusal.getMessage(), path, imported);
        }
    }

    private OpenApiEditResult applyCommand(ImportResult imported, EditCommand command) {
        if (imported.status() != ImportStatus.IMPORTED) {
            throw new IllegalArgumentException("Only imported OpenAPI documents can be edited");
        }
        EditResult edit = documentEditor.apply(imported.document().orElseThrow(), command);
        if (!(edit instanceof EditSucceeded succeeded)) {
            if (edit instanceof EditRejected rejected
                    && rejected.issues().stream().anyMatch(issue -> issue.code() == EditCode.REFERENCED_TARGET)) {
                return sourceDeletionRejection(imported, command).orElseGet(
                        () -> new OpenApiEditResult.Rejected((EditRejected) edit, imported)
                );
            }
            return new OpenApiEditResult.Rejected((EditRejected) edit, imported);
        }
        SourceFormat format = imported.format().orElseThrow();
        OpenApiVersion version = OpenApiVersion.supported(imported.version().orElseThrow()).orElseThrow();
        if (succeeded.changeSet().changes().stream().anyMatch(this::changesSchemaReferenceScope)) {
            SchemaIdScopeDetector.rejectIfPresent(imported.source(), format);
        }
        Optional<OpenApiEditResult> sourceReferenceRejection = rejectUnmappedIncomingReferences(
                imported,
                succeeded
        );
        if (sourceReferenceRejection.isPresent()) {
            return sourceReferenceRejection.orElseThrow();
        }
        SourceDocument source = sourceAfterEdit(imported, command, succeeded, format, version);
        ImportResult checked = importer.importDocument(source.content(), ImportOptions.withFormat(format));
        if (checked.status() != ImportStatus.IMPORTED || !checked.version().equals(imported.version())) {
            throw new IllegalStateException(
                    "Patched OpenAPI document could not be imported: " + checked.diagnostics()
            );
        }
        new SchemaModelConsistencyVerifier().verify(
                succeeded.document(),
                checked.document().orElseThrow(),
                version
        );
        if (!succeeded.changeSet().changes().isEmpty()) {
            Change change = succeeded.changeSet().changes().getFirst();
            switch (command) {
                case AddSchemaComponent ignored -> verifyAddition(imported, checked, change, version);
                case AddSchemaProperty ignored -> verifyAddition(imported, checked, change, version);
                case UpdateSchemaObject ignored -> verifyUpdate(checked, change, version);
                case RenameSchemaComponent ignored -> verifyStructuralEdit(checked, change);
                case RenameSchemaProperty ignored -> verifyStructuralEdit(checked, change);
                case DeleteSchemaComponent ignored -> verifyStructuralEdit(checked, change);
                case DeleteSchemaProperty ignored -> verifyStructuralEdit(checked, change);
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
        return new OpenApiEditResult.Applied(succeeded, source, state);
    }

    private boolean changesSchemaReferenceScope(Change change) {
        return change instanceof SchemaComponentRenamed
                || change instanceof SchemaPropertyRenamed
                || change instanceof SchemaComponentDeleted
                || change instanceof SchemaPropertyDeleted;
    }

    private SourceDocument sourceAfterEdit(
            ImportResult imported,
            EditCommand command,
            EditSucceeded succeeded,
            SourceFormat format,
            OpenApiVersion version
    ) {
        return switch (command) {
            case RenameSchemaComponent ignored -> patchRename(imported, succeeded, format);
            case RenameSchemaProperty ignored -> patchRename(imported, succeeded, format);
            case DeleteSchemaComponent ignored -> patchDeletion(imported, succeeded, format);
            case DeleteSchemaProperty ignored -> patchDeletion(imported, succeeded, format);
            case UpdateSchemaObject ignored -> new SchemaFieldPatcher().apply(
                    imported.source(), format, version,
                    (SchemaObjectUpdated) succeeded.changeSet().changes().getFirst()
            );
            case AddSchemaComponent ignored -> new OpenApiAdditionPatcher().apply(imported, succeeded.changeSet());
            case AddSchemaProperty ignored -> new OpenApiAdditionPatcher().apply(imported, succeeded.changeSet());
        };
    }

    private SourceDocument patchRename(ImportResult imported, EditSucceeded succeeded, SourceFormat format) {
        return new OpenApiSourcePatcher().apply(imported.source(), format, succeeded.changeSet());
    }

    private SourceDocument patchDeletion(ImportResult imported, EditSucceeded succeeded, SourceFormat format) {
        return new OpenApiSchemaDeletionPatcher().remove(
                imported.source(), format, deletionPath(succeeded.changeSet().changes().getFirst())
        );
    }

    private Optional<OpenApiEditResult> rejectUnmappedIncomingReferences(
            ImportResult imported,
            EditSucceeded succeeded
    ) {
        if (succeeded.changeSet().changes().isEmpty()) {
            return Optional.empty();
        }
        if (!isDeletion(succeeded.changeSet().changes().getFirst())) {
            return Optional.empty();
        }
        Change change = succeeded.changeSet().changes().getFirst();
        DocumentPath targetPath = deletionPath(change);
        return rejectIncomingReferences(imported, targetPath, deletionDescription(change));
    }

    private Optional<OpenApiEditResult> sourceDeletionRejection(ImportResult imported, EditCommand command) {
        if (SchemaIdScopeDetector.containsSchemaId(imported.source(), imported.format().orElseThrow())) {
            return Optional.empty();
        }
        DocumentIndex index = DocumentIndex.build(imported.document().orElseThrow());
        DocumentPath path;
        if (command instanceof DeleteSchemaComponent component) {
            path = index.findById(component.componentId()).orElseThrow().path();
        } else if (command instanceof DeleteSchemaProperty property) {
            path = index.findById(property.propertyId()).orElseThrow().path();
        } else {
            return Optional.empty();
        }
        return rejectIncomingReferences(imported, path, path.toString());
    }

    private Optional<OpenApiEditResult> rejectIncomingReferences(
            ImportResult imported,
            DocumentPath targetPath,
            String target
    ) {
        List<DocumentPath> references = new OpenApiSchemaDeletionPatcher().incomingReferences(
                imported.source(),
                imported.format().orElseThrow(),
                targetPath
        );
        if (references.isEmpty()) {
            return Optional.empty();
        }
        String locations = references.stream().map(Object::toString).collect(Collectors.joining(", "));
        EditRejected rejection = new EditRejected(List.of(new EditIssue(
                EditCode.REFERENCED_TARGET,
                "Cannot delete " + target + "; incoming references: " + locations
        )));
        return Optional.of(new OpenApiEditResult.Rejected(rejection, imported));
    }

    private String deletionDescription(Change change) {
        if (change instanceof SchemaComponentDeleted component) {
            return "schema component '" + component.name() + "'";
        }
        if (change instanceof SchemaPropertyDeleted property) {
            return "schema property '" + property.name() + "'";
        }
        throw new IllegalStateException("Unsupported schema deletion");
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
        throw new IllegalStateException("Unsupported schema deletion");
    }

    private void verifyStructuralEdit(ImportResult imported, Change change) {
        DocumentIndex index = DocumentIndex.build(imported.document().orElseThrow());
        if (change instanceof SchemaPropertyRenamed property) {
            DocumentPath oldPath = property.schemaPath().child(PROPERTIES_FIELD).child(property.oldName());
            DocumentPath newPath = property.schemaPath().child(PROPERTIES_FIELD).child(property.newName());
            if (index.findByPath(oldPath).isPresent() || index.findByPath(newPath).isEmpty()) {
                throw new IllegalStateException("Renamed schema property was not preserved in source");
            }
            return;
        }
        if (isDeletion(change)) {
            if (index.findByPath(deletionPath(change)).isPresent()) {
                throw new IllegalStateException("Deleted schema element was not removed from source");
            }
            return;
        }
        if (change instanceof SchemaComponentRenamed component) {
            DocumentPath oldPath = DocumentPath.schemaComponent(component.oldName());
            DocumentPath newPath = DocumentPath.schemaComponent(component.newName());
            if (index.findByPath(oldPath).isPresent() || index.findByPath(newPath).isEmpty()) {
                throw new IllegalStateException("Renamed schema component was not preserved in source");
            }
            return;
        }
        throw new IllegalStateException("Unsupported structural schema change");
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
            throw new IllegalStateException("Unsupported schema addition");
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
            throw new IllegalStateException("Unsupported schema update");
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

    private DocumentPath commandPath(ImportResult imported, EditCommand command) {
        DocumentIndex index = DocumentIndex.build(imported.document().orElseThrow());
        return switch (command) {
            case AddSchemaComponent addition -> DocumentPath.schemaComponent(addition.component().getName());
            case AddSchemaProperty addition -> index.findById(addition.schemaId()).orElseThrow().path()
                    .child(PROPERTIES_FIELD).child(addition.property().getName());
            case RenameSchemaComponent rename -> index.findById(rename.componentId()).orElseThrow().path();
            case RenameSchemaProperty rename -> index.findById(rename.propertyId()).orElseThrow().path();
            case DeleteSchemaComponent deletion -> index.findById(deletion.componentId()).orElseThrow().path();
            case DeleteSchemaProperty deletion -> index.findById(deletion.propertyId()).orElseThrow().path();
            case UpdateSchemaObject update -> index.findById(update.schema().getId()).orElseThrow().path();
        };
    }
}
