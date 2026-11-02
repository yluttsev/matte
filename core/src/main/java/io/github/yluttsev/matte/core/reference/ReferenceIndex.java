package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReferenceIndex {

    private final DocumentIndex document;
    private final List<ReferenceUsage<?>> usages;
    private final Map<DocumentPath, ReferenceUsage<?>> bySource;
    private final Map<DocumentPath, List<ReferenceUsage<?>>> byTarget;

    ReferenceIndex(DocumentIndex document, List<ReferenceUsage<?>> usages) {
        this.document = Objects.requireNonNull(document, "document");
        this.usages = List.copyOf(usages);
        Map<DocumentPath, ReferenceUsage<?>> bySource = new LinkedHashMap<>();
        Map<DocumentPath, List<ReferenceUsage<?>>> byTarget = new LinkedHashMap<>();
        for (ReferenceUsage<?> usage : usages) {
            if (bySource.putIfAbsent(usage.sourcePath(), usage) != null) {
                throw new IllegalArgumentException("Duplicate reference source: " + usage.sourcePath());
            }
            usage.resolution().traversedPaths().stream().distinct().forEach(path ->
                    byTarget.computeIfAbsent(path, ignored -> new ArrayList<>()).add(usage));
        }
        this.bySource = Collections.unmodifiableMap(bySource);
        byTarget.replaceAll((path, references) -> List.copyOf(references));
        this.byTarget = Collections.unmodifiableMap(byTarget);
    }

    public List<ReferenceUsage<?>> usages() {
        return usages;
    }

    public List<ReferenceUsage<?>> unresolvedUsages() {
        return usages.stream().filter(usage -> usage.resolution() instanceof UnresolvedReference<?>).toList();
    }

    public Optional<ReferenceUsage<?>> findBySource(DocumentPath path) {
        return Optional.ofNullable(bySource.get(Objects.requireNonNull(path, "path")));
    }

    public List<ReferenceUsage<?>> referencesTo(ElementId id) {
        return document.findById(id).map(entry -> referencesTo(entry.path())).orElseGet(List::of);
    }

    public List<ReferenceUsage<?>> referencesTo(DocumentPath path) {
        Objects.requireNonNull(path, "path");
        return byTarget.getOrDefault(path, List.of());
    }

    public List<ReferenceUsage<?>> referencesToSubtree(DocumentPath path) {
        Objects.requireNonNull(path, "path");
        return usages.stream().filter(usage -> usage.resolution().traversedPaths().stream()
                .anyMatch(target -> target.startsWith(path))).toList();
    }

    public List<ReferenceUsage<?>> referencesFromSubtree(DocumentPath path) {
        Objects.requireNonNull(path, "path");
        return usages.stream().filter(usage -> usage.sourcePath().startsWith(path)).toList();
    }
}
