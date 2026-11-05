package io.github.yluttsev.matte.core.editing;

import java.util.List;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyDeleted;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceUsage;

public final class DeleteSchemaPropertyHandler implements EditCommandHandler<DeleteSchemaProperty> {

    @Override
    public Class<DeleteSchemaProperty> commandType() {
        return DeleteSchemaProperty.class;
    }

    @Override
    public EditResult handle(EditContext context, DeleteSchemaProperty command) {
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
        DocumentPath propertyPath = schemaEntry.path().child("properties").child(property.getName());
        List<ReferenceUsage<?>> references = SchemaDeletionReferences.externalReferencesTo(index, propertyPath);
        if (!references.isEmpty()) {
            return SchemaDeletionReferences.referencedTarget("schema property '" + property.getName() + "'",
                    references);
        }

        schema.removeProperty(access, property.getId());
        return EditResults.succeeded(document, List.of(new SchemaPropertyDeleted(
                schema.getId(),
                property.getId(),
                property.getName(),
                propertyPath
        )));
    }
}
