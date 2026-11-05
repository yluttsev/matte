package io.github.yluttsev.matte.core.editing;

import java.util.List;
import java.util.stream.Collectors;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndexBuilder;
import io.github.yluttsev.matte.core.reference.ReferenceUsage;

final class SchemaDeletionReferences {

    private SchemaDeletionReferences() {
    }

    static List<ReferenceUsage<?>> externalReferencesTo(
            DocumentIndex index,
            DocumentPath targetPath
    ) {
        ReferenceIndex references = new ReferenceIndexBuilder().build(index);
        return references.referencesToSubtree(targetPath).stream()
                .filter(usage -> !usage.sourcePath().startsWith(targetPath))
                .toList();
    }

    static EditResult referencedTarget(
            String target,
            List<ReferenceUsage<?>> references
    ) {
        String locations = references.stream()
                .map(usage -> usage.sourcePath() + " (" + usage.kind() + ": " + usage.reference() + ")")
                .collect(Collectors.joining(", "));
        return EditResults.rejected(EditCode.REFERENCED_TARGET,
                "Cannot delete " + target + "; incoming references: " + locations);
    }
}
