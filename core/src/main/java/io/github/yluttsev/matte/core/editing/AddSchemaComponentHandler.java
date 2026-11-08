package io.github.yluttsev.matte.core.editing;

import java.util.List;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentAdded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

public final class AddSchemaComponentHandler implements EditCommandHandler<AddSchemaComponent> {

    @Override
    public Class<AddSchemaComponent> commandType() {
        return AddSchemaComponent.class;
    }

    @Override
    public EditResult handle(EditContext context, AddSchemaComponent command) {
        ApiDocument document = context.document();
        DocumentIndex index = context.index();
        MutationAccess access = context.access();
        SchemaComponent component = command.component();
        if (!ComponentNameValidator.isValid(component.getName())) {
            return EditResults.rejected(EditCode.INVALID_NAME, ComponentNameValidator.INVALID_NAME_MESSAGE);
        }
        if (SchemaPropertyChecks.containsBlankPropertyName(component)) {
            return EditResults.rejected(EditCode.INVALID_NAME, "Property name must not be blank");
        }
        if (SchemaComponentNames.hasSchemaComponentNamed(document, component.getName())) {
            return EditResults.rejected(EditCode.NAME_CONFLICT,
                    "Schema component already exists: " + component.getName());
        }
        List<EditIssue> conflicts = SchemaIdChecks.idConflicts(index, component);
        if (!conflicts.isEmpty()) {
            return new EditRejected(conflicts);
        }
        document.getComponents().addSchemaComponent(access, component);
        return EditResults.succeeded(document, List.of(new SchemaComponentAdded(
                component.getId(),
                component.getName(),
                component.getSchema()
        )));
    }
}
