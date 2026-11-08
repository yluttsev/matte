package io.github.yluttsev.matte.core.editing;

import java.util.ArrayList;
import java.util.List;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentRenamed;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndexBuilder;

public final class RenameSchemaComponentHandler implements EditCommandHandler<RenameSchemaComponent> {

    @Override
    public Class<RenameSchemaComponent> commandType() {
        return RenameSchemaComponent.class;
    }

    @Override
    public EditResult handle(EditContext context, RenameSchemaComponent command) {
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
        if (!ComponentNameValidator.isValid(command.newName())) {
            return EditResults.rejected(EditCode.INVALID_NAME, ComponentNameValidator.INVALID_NAME_MESSAGE);
        }
        if (component.getName().equals(command.newName())) {
            return EditResults.succeeded(document, List.of());
        }
        if (SchemaComponentNames.hasSchemaComponentNamed(document, command.newName())) {
            return EditResults.rejected(EditCode.NAME_CONFLICT,
                    "Schema component already exists: " + command.newName());
        }
        SchemaReferenceRename rename = new SchemaReferenceRename(component.getName(), command.newName(), index);
        ReferenceIndex references = new ReferenceIndexBuilder().build(index);
        List<Change> changes = new ArrayList<>();
        changes.add(new SchemaComponentRenamed(component.getId(), component.getName(), command.newName()));
        changes.addAll(rename.changes(references));
        component.rename(access, command.newName());
        new ModelWalker().walk(document, (node, path) -> {
            if (node instanceof ReferenceOwner owner) {
                rename.rewriteOwner(access, owner);
            }
        });
        return EditResults.succeeded(document, changes);
    }
}
