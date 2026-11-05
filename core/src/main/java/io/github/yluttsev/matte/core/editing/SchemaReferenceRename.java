package io.github.yluttsev.matte.core.editing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceKind;
import io.github.yluttsev.matte.core.reference.ReferenceUris;
import io.github.yluttsev.matte.core.reference.ReferenceUsage;

final class SchemaReferenceRename {

    private final String oldName;
    private final String newName;
    private final DocumentPath oldPath;
    private final DocumentPath newPath;
    private final DocumentIndex document;

    SchemaReferenceRename(String oldName, String newName, DocumentIndex document) {
        this.oldName = oldName;
        this.newName = newName;
        oldPath = DocumentPath.schemaComponent(oldName);
        newPath = DocumentPath.schemaComponent(newName);
        this.document = document;
    }

    String reference(String value) {
        try {
            URI uri = new URI(value);
            if (ReferenceUris.isExternal(uri) || uri.getRawFragment() == null) {
                return value;
            }
            DocumentPath target = DocumentPath.fromFragment("#" + uri.getRawFragment());
            if (target.startsWith(oldPath)) {
                return relocated(target).toFragment();
            }
            return value;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return value;
        }
    }

    String mapping(String value) {
        if (value.equals(oldName)) {
            return newName;
        }
        if (document.findByPath(DocumentPath.schemaComponent(value)).isPresent()) {
            return value;
        }
        return reference(value);
    }

    void rewriteOwner(MutationAccess access, ReferenceOwner owner) {
        UnaryOperator<String> transform;
        if (owner instanceof Discriminator) {
            transform = this::mapping;
        } else {
            transform = this::reference;
        }
        owner.rewriteReferences(access, transform);
    }

    List<Change> changes(ReferenceIndex index) {
        List<Change> changes = new ArrayList<>();
        for (ReferenceUsage<?> usage : index.usages()) {
            String updated = rewriteUsage(usage);
            if (!updated.equals(usage.reference())) {
                changes.add(new ReferenceChanged(
                        usage.sourcePath(),
                        relocated(usage.sourcePath()),
                        usage.reference(),
                        updated,
                        usage.kind()
                ));
            }
        }
        return changes;
    }

    private String rewriteUsage(ReferenceUsage<?> usage) {
        if (usage.kind() == ReferenceKind.DISCRIMINATOR_MAPPING) {
            return mapping(usage.reference());
        }
        return reference(usage.reference());
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
