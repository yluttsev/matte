package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ReferenceResolver {

    public ReferenceResolution<Schema> resolve(DocumentIndex index, SchemaReference reference) {
        Objects.requireNonNull(reference, "reference");
        return resolve(index, reference.value(), Schema.class);
    }

    public <T extends CopyableModel<?>> ReferenceResolution<T> resolve(
            DocumentIndex index,
            String reference,
            Class<T> expectedType
    ) {
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(expectedType, "expectedType");
        List<DocumentPath> traversed = new ArrayList<>();
        Set<DocumentPath> visited = new HashSet<>();
        String current = reference;
        while (true) {
            URI uri;
            try {
                uri = new URI(current);
            } catch (URISyntaxException exception) {
                return new UnresolvedReference<>(ReferenceFailure.INVALID_REFERENCE, traversed);
            }
            if (uri.getScheme() != null || uri.getRawAuthority() != null || uri.getRawQuery() != null
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty()) {
                return new UnresolvedReference<>(ReferenceFailure.EXTERNAL_REFERENCE, traversed);
            }
            String fragment = uri.getRawFragment();
            if (fragment != null && !fragment.isEmpty() && !uri.getFragment().startsWith("/")) {
                return new UnresolvedReference<>(ReferenceFailure.UNSUPPORTED_ANCHOR, traversed);
            }
            DocumentPath path;
            try {
                path = fragment == null ? DocumentPath.root() : DocumentPath.fromFragment("#" + fragment);
            } catch (IllegalArgumentException exception) {
                return new UnresolvedReference<>(ReferenceFailure.INVALID_REFERENCE, traversed);
            }
            if (!visited.add(path)) {
                return new UnresolvedReference<>(ReferenceFailure.CYCLIC_REFERENCE, traversed);
            }
            traversed.add(path);
            DocumentEntry entry = index.findByPath(path).orElse(null);
            if (entry == null) {
                return new UnresolvedReference<>(ReferenceFailure.TARGET_NOT_FOUND, traversed);
            }
            if (expectedType.isInstance(entry.value())) {
                return new ResolvedReference<>(expectedType.cast(entry.value()), path, traversed);
            }
            if (entry.value() instanceof ObjectReference<?> alias
                    && ReferenceTypes.at(path).filter(expectedType::isAssignableFrom).isPresent()) {
                current = alias.value();
            } else {
                return new UnresolvedReference<>(ReferenceFailure.TYPE_MISMATCH, traversed);
            }
        }
    }
}
