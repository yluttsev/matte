package io.github.yluttsev.matte.core.editing;

import java.util.ArrayList;
import java.util.List;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyRenamed;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndexBuilder;

public final class RenameSchemaPropertyHandler implements EditCommandHandler<RenameSchemaProperty> {

    @Override
    public Class<RenameSchemaProperty> commandType() {
        return RenameSchemaProperty.class;
    }

    @Override
    public EditResult handle(EditContext context, RenameSchemaProperty command) {
        ApiDocument document = context.document();
        DocumentIndex index = context.index();
        MutationAccess access = context.access();
        DocumentEntry schemaEntry = index.findById(command.schemaId()).orElse(null);
        if (schemaEntry == null) {
            return EditResults.rejected(EditCode.TARGET_NOT_FOUND, "Schema not found: " + command.schemaId().value());
        }
        if (!(schemaEntry.value() instanceof SchemaObject schema)) {
            return EditResults.rejected(EditCode.TARGET_TYPE_MISMATCH, "Target must be a SchemaObject");
        }
        SchemaProperty property = SchemaPropertyChecks.findProperty(schema, command.propertyId());
        if (property == null) {
            return EditResults.rejected(EditCode.TARGET_NOT_FOUND,
                    "Schema property not found: " + command.propertyId().value());
        }
        if (command.newName().isBlank()) {
            return EditResults.rejected(EditCode.INVALID_NAME, "Schema property name must not be blank");
        }
        if (property.getName().equals(command.newName())) {
            return EditResults.succeeded(document, List.of());
        }
        if (SchemaPropertyChecks.hasPropertyNamed(schema, command.newName())) {
            return EditResults.rejected(EditCode.NAME_CONFLICT, "Schema property already exists: " + command.newName());
        }

        DocumentPath oldPath = schemaEntry.path().child("properties").child(property.getName());
        DocumentPath newPath = schemaEntry.path().child("properties").child(command.newName());
        SchemaPointerRename rename = new SchemaPointerRename(oldPath, newPath);
        ReferenceIndex references = new ReferenceIndexBuilder().build(index);
        List<Change> changes = new ArrayList<>();
        changes.add(new SchemaPropertyRenamed(
                schema.getId(),
                property.getId(),
                schemaEntry.path(),
                property.getName(),
                command.newName()
        ));
        changes.addAll(rename.changes(references));
        schema.renameProperty(access, property.getId(), command.newName());
        new ModelWalker().walk(document, (node, path) -> {
            if (node instanceof ReferenceOwner owner) {
                rename.rewriteOwner(access, owner);
            }
        });
        return EditResults.succeeded(document, changes);
    }
}
