package io.github.yluttsev.matte.core.editing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

final class SchemaIdChecks {

    private SchemaIdChecks() {
    }

    static List<EditIssue> idConflicts(DocumentIndex index, CopyableModel<?> addition) {
        return idConflicts(index, addition, Set.of());
    }

    static List<EditIssue> idConflicts(
            DocumentIndex index,
            CopyableModel<?> addition,
            Set<ElementId> reusableIds
    ) {
        Set<ElementId> seen = new HashSet<>();
        Set<ElementId> conflicts = new HashSet<>();
        List<EditIssue> issues = new ArrayList<>();
        new ModelWalker().walk(addition, (node, path) -> {
            if (node instanceof IdentifiedElement element) {
                ElementId id = element.getId();
                boolean duplicateInAddition = !seen.add(id);
                boolean existsInDocument = index.findById(id).isPresent() && !reusableIds.contains(id);
                if ((duplicateInAddition || existsInDocument) && conflicts.add(id)) {
                    issues.add(new EditIssue(EditCode.ID_CONFLICT, "Element ID already exists: " + id.value()));
                }
            }
        });
        return issues;
    }

    static Set<ElementId> elementIds(CopyableModel<?> model) {
        Set<ElementId> ids = new HashSet<>();
        new ModelWalker().walk(model, (node, path) -> {
            if (node instanceof IdentifiedElement element) {
                ids.add(element.getId());
            }
        });
        return ids;
    }
}
