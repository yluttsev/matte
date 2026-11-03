package io.github.yluttsev.matte.core.editing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ChangeSet;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentAdded;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentRenamed;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyAdded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndexBuilder;

final class SchemaCommandHandler {

    EditResult apply(
            ApiDocument document,
            DocumentIndex index,
            MutationAccess access,
            EditCommand command
    ) {
        if (command instanceof AddSchemaComponent add) {
            return addComponent(document, index, access, add);
        }
        if (command instanceof AddSchemaProperty add) {
            return addProperty(document, index, access, add);
        }
        if (command instanceof RenameSchemaComponent rename) {
            return renameComponent(document, index, access, rename);
        }
        throw new IllegalArgumentException("Unsupported command: " + command.getClass());
    }

    private EditResult addComponent(
            ApiDocument document,
            DocumentIndex index,
            MutationAccess access,
            AddSchemaComponent command
    ) {
        SchemaComponent component = command.component();
        if (component.getName().isBlank()) {
            return rejected(EditCode.INVALID_NAME, "Schema component name must not be blank");
        }
        if (hasBlankPropertyName(component)) {
            return rejected(EditCode.INVALID_NAME, "Property name must not be blank");
        }
        if (hasName(document, component.getName())) {
            return rejected(EditCode.NAME_CONFLICT, "Schema component already exists: " + component.getName());
        }
        List<EditIssue> conflicts = idConflicts(index, component);
        if (!conflicts.isEmpty()) {
            return new EditRejected(conflicts);
        }
        document.getComponents().addSchemaComponent(access, component);
        return succeeded(document, List.of(new SchemaComponentAdded(
                component.getId(), component.getName(), component.getSchema())));
    }

    private EditResult addProperty(
            ApiDocument document,
            DocumentIndex index,
            MutationAccess access,
            AddSchemaProperty command
    ) {
        DocumentEntry entry = index.findById(command.schemaId()).orElse(null);
        if (entry == null) {
            return rejected(EditCode.TARGET_NOT_FOUND, "Schema not found: " + command.schemaId().value());
        }
        if (!(entry.value() instanceof SchemaObject schema)) {
            return rejected(EditCode.TARGET_TYPE_MISMATCH, "Target must be a SchemaObject");
        }
        SchemaProperty property = command.property();
        if (hasBlankPropertyName(property)) {
            return rejected(EditCode.INVALID_NAME, "Property name must not be blank");
        }
        if (schema.getProperties().stream().anyMatch(existing -> existing.getName().equals(property.getName()))) {
            return rejected(EditCode.NAME_CONFLICT, "Schema property already exists: " + property.getName());
        }
        List<EditIssue> conflicts = idConflicts(index, property);
        if (!conflicts.isEmpty()) {
            return new EditRejected(conflicts);
        }
        schema.addProperty(access, property);
        return succeeded(document, List.of(new SchemaPropertyAdded(
                schema.getId(), property.getId(), property.getName(), property.getSchema())));
    }

    private EditResult renameComponent(
            ApiDocument document,
            DocumentIndex index,
            MutationAccess access,
            RenameSchemaComponent command
    ) {
        DocumentEntry entry = index.findById(command.componentId()).orElse(null);
        if (entry == null) {
            return rejected(EditCode.TARGET_NOT_FOUND, "Schema component not found: " + command.componentId().value());
        }
        if (!(entry.value() instanceof SchemaComponent component)) {
            return rejected(EditCode.TARGET_TYPE_MISMATCH, "Target must be a SchemaComponent");
        }
        if (command.newName().isBlank()) {
            return rejected(EditCode.INVALID_NAME, "Schema component name must not be blank");
        }
        if (component.getName().equals(command.newName())) {
            return succeeded(document, List.of());
        }
        if (hasName(document, command.newName())) {
            return rejected(EditCode.NAME_CONFLICT, "Schema component already exists: " + command.newName());
        }
        SchemaReferenceRename rename = new SchemaReferenceRename(component.getName(), command.newName());
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
        return succeeded(document, changes);
    }

    private List<EditIssue> idConflicts(DocumentIndex index, CopyableModel<?> addition) {
        Set<ElementId> seen = new HashSet<>();
        Set<ElementId> conflicts = new HashSet<>();
        List<EditIssue> issues = new ArrayList<>();
        new ModelWalker().walk(addition, (node, path) -> {
            if (node instanceof IdentifiedElement element) {
                ElementId id = element.getId();
                boolean duplicateInAddition = !seen.add(id);
                boolean existsInDocument = index.findById(id).isPresent();
                if ((duplicateInAddition || existsInDocument) && conflicts.add(id)) {
                    issues.add(new EditIssue(EditCode.ID_CONFLICT, "Element ID already exists: " + id.value()));
                }
            }
        });
        return issues;
    }

    private boolean hasBlankPropertyName(CopyableModel<?> addition) {
        List<SchemaProperty> invalid = new ArrayList<>();
        new ModelWalker().walk(addition, (node, path) -> {
            if (node instanceof SchemaProperty property && property.getName().isBlank()) {
                invalid.add(property);
            }
        });
        return !invalid.isEmpty();
    }

    private boolean hasName(ApiDocument document, String name) {
        return document.getComponents().getSchemas().stream().anyMatch(component -> component.getName().equals(name));
    }

    private EditSucceeded succeeded(ApiDocument document, List<Change> changes) {
        return new EditSucceeded(document, new ChangeSet(changes));
    }

    private EditRejected rejected(EditCode code, String message) {
        return new EditRejected(List.of(new EditIssue(code, message)));
    }
}
