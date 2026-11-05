package io.github.yluttsev.matte.core.editing;

import java.util.List;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentDeleted;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.reference.ReferenceUsage;

public final class DeleteSchemaComponentHandler implements EditCommandHandler<DeleteSchemaComponent> {

    @Override
    public Class<DeleteSchemaComponent> commandType() {
        return DeleteSchemaComponent.class;
    }

    @Override
    public EditResult handle(EditContext context, DeleteSchemaComponent command) {
        ApiDocument document = context.document();
        DocumentIndex index = context.index();
        MutationAccess access = context.access();
        DocumentEntry entry = index.findById(command.componentId()).orElse(null);
        if (entry == null) {
            return EditResults.rejected(EditCode.TARGET_NOT_FOUND,
                    "Schema component not found: " + command.componentId().value());
        }
        if (!(entry.value() instanceof SchemaComponent component)) {
            return EditResults.rejected(EditCode.TARGET_TYPE_MISMATCH, "Target must be a SchemaComponent");
        }
        List<ReferenceUsage<?>> references = SchemaDeletionReferences.externalReferencesTo(index, entry.path());
        if (!references.isEmpty()) {
            return SchemaDeletionReferences.referencedTarget("schema component '" + component.getName() + "'",
                    references);
        }

        document.getComponents().removeSchemaComponent(access, component);
        return EditResults.succeeded(document, List.of(new SchemaComponentDeleted(
                component.getId(),
                component.getName(),
                entry.path()
        )));
    }
}
