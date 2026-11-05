package io.github.yluttsev.matte.core.editing;

import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.change.SchemaObjectUpdated;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

public final class UpdateSchemaObjectHandler implements EditCommandHandler<UpdateSchemaObject> {

    @Override
    public Class<UpdateSchemaObject> commandType() {
        return UpdateSchemaObject.class;
    }

    @Override
    public EditResult handle(EditContext context, UpdateSchemaObject command) {
        ApiDocument document = context.document();
        DocumentIndex index = context.index();
        MutationAccess access = context.access();
        SchemaObject replacement = command.schema();
        DocumentEntry entry = index.findById(replacement.getId()).orElse(null);
        if (entry == null) {
            return EditResults.rejected(EditCode.TARGET_NOT_FOUND, "Schema not found: " + replacement.getId().value());
        }
        if (!(entry.value() instanceof SchemaObject schema)) {
            return EditResults.rejected(EditCode.TARGET_TYPE_MISMATCH, "Target must be a SchemaObject");
        }
        if (SchemaPropertyChecks.containsBlankPropertyName(replacement)) {
            return EditResults.rejected(EditCode.INVALID_NAME, "Property name must not be blank");
        }
        Set<ElementId> reusableIds = SchemaIdChecks.elementIds(schema);
        List<EditIssue> conflicts = SchemaIdChecks.idConflicts(index, replacement, reusableIds);
        if (!conflicts.isEmpty()) {
            return new EditRejected(conflicts);
        }

        SchemaObject before = schema.copy();
        schema.replaceWith(access, replacement);
        SchemaObjectUpdated change = new SchemaObjectUpdated(schema.getId(), entry.path(), before, schema);
        return EditResults.succeeded(document, List.of(change));
    }
}
