package io.github.yluttsev.matte.core.editing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceKind;
import io.github.yluttsev.matte.core.reference.ReferenceUsage;

final class SchemaReferenceRename {

    private final String oldName;
    private final String newName;
    private final DocumentPath oldPath;
    private final DocumentPath newPath;

    SchemaReferenceRename(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
        oldPath = DocumentPath.root().child("components").child("schemas").child(oldName);
        newPath = DocumentPath.root().child("components").child("schemas").child(newName);
    }

    String reference(String value) {
        try {
            URI uri = new URI(value);
            if (uri.getScheme() != null || uri.getRawAuthority() != null || uri.getRawQuery() != null
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty() || uri.getRawFragment() == null) {
                return value;
            }
            DocumentPath target = DocumentPath.fromFragment("#" + uri.getRawFragment());
            return target.startsWith(oldPath) ? relocated(target).toFragment() : value;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return value;
        }
    }

    String mapping(String value) {
        return value.equals(oldName) ? newName : reference(value);
    }

    List<Change> changes(ReferenceIndex index) {
        List<Change> changes = new ArrayList<>();
        for (ReferenceUsage<?> usage : index.usages()) {
            String updated = usage.kind() == ReferenceKind.DISCRIMINATOR_MAPPING
                    ? mapping(usage.reference()) : reference(usage.reference());
            if (!updated.equals(usage.reference())) {
                changes.add(new ReferenceChanged(usage.sourcePath(), relocated(usage.sourcePath()),
                        usage.reference(), updated, usage.kind()));
            }
        }
        return changes;
    }

    private DocumentPath relocated(DocumentPath path) {
        if (!path.startsWith(oldPath)) {
            return path;
        }
        List<String> segments = new ArrayList<>(newPath.segments());
        segments.addAll(path.segments().subList(oldPath.segments().size(), path.segments().size()));
        return new DocumentPath(segments);
    }
}
