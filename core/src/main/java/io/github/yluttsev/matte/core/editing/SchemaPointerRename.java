package io.github.yluttsev.matte.core.editing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceUris;
import io.github.yluttsev.matte.core.reference.ReferenceUsage;

final class SchemaPointerRename {

    private final DocumentPath oldPath;
    private final DocumentPath newPath;

    SchemaPointerRename(
            DocumentPath oldPath,
            DocumentPath newPath
    ) {
        this.oldPath = oldPath;
        this.newPath = newPath;
    }

    String reference(String value) {
        try {
            URI uri = new URI(value);
            if (ReferenceUris.isExternal(uri) || uri.getRawFragment() == null) {
                return value;
            }
            DocumentPath target = DocumentPath.fromFragment("#" + uri.getRawFragment());
            if (target.startsWith(oldPath)) {
                return relocate(target).toFragment();
            }
            return value;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return value;
        }
    }

    void rewriteOwner(
            MutationAccess access,
            ReferenceOwner owner
    ) {
        UnaryOperator<String> transform = this::reference;
        owner.rewriteReferences(access, transform);
    }

    List<Change> changes(ReferenceIndex index) {
        List<Change> changes = new ArrayList<>();
        for (ReferenceUsage<?> usage : index.usages()) {
            String updated = reference(usage.reference());
            if (!updated.equals(usage.reference())) {
                changes.add(new ReferenceChanged(
                        usage.sourcePath(),
                        relocate(usage.sourcePath()),
                        usage.reference(),
                        updated,
                        usage.kind()
                ));
            }
        }
        return changes;
    }

    private DocumentPath relocate(DocumentPath path) {
        if (!path.startsWith(oldPath)) {
            return path;
        }
        List<String> segments = new ArrayList<>(newPath.segments());
        segments.addAll(path.segments().subList(oldPath.segments().size(), path.segments().size()));
        return new DocumentPath(segments);
    }
}
