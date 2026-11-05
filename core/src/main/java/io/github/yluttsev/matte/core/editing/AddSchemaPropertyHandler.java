package io.github.yluttsev.matte.core.editing;

import java.util.List;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyAdded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

public final class AddSchemaPropertyHandler implements EditCommandHandler<AddSchemaProperty> {

    @Override
    public Class<AddSchemaProperty> commandType() {
        return AddSchemaProperty.class;
    }

    @Override
    public EditResult handle(EditContext context, AddSchemaProperty command) {
        ApiDocument document = context.document();
        DocumentIndex index = context.index();
        MutationAccess access = context.access();
        DocumentEntry entry = index.findById(command.schemaId()).orElse(null);
        if (entry == null) {
            return EditResults.rejected(EditCode.TARGET_NOT_FOUND, "Schema not found: " + command.schemaId().value());
        }
        if (!(entry.value() instanceof SchemaObject schema)) {
            return EditResults.rejected(EditCode.TARGET_TYPE_MISMATCH, "Target must be a SchemaObject");
        }
        SchemaProperty property = command.property();
        if (SchemaPropertyChecks.containsBlankPropertyName(property)) {
            return EditResults.rejected(EditCode.INVALID_NAME, "Property name must not be blank");
        }
        if (schema.getProperties().stream().anyMatch(existing -> existing.getName().equals(property.getName()))) {
            return EditResults.rejected(EditCode.NAME_CONFLICT,
                    "Schema property already exists: " + property.getName());
        }
        List<EditIssue> conflicts = SchemaIdChecks.idConflicts(index, property);
        if (!conflicts.isEmpty()) {
            return new EditRejected(conflicts);
        }
        schema.addProperty(access, property);
        return EditResults.succeeded(document, List.of(new SchemaPropertyAdded(
                schema.getId(),
                property.getId(),
                property.getName(),
                property.getSchema()
        )));
    }
}
