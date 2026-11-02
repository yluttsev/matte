package io.github.yluttsev.matte.core.navigation;

import io.github.yluttsev.matte.core.construction.DocumentStructureValidator;
import io.github.yluttsev.matte.core.construction.traversal.ModelWalker;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DocumentIndex {

    private final List<DocumentEntry> entries;
    private final Map<ElementId, DocumentEntry> byId;
    private final Map<DocumentPath, DocumentEntry> byPath;

    private DocumentIndex(
            List<DocumentEntry> entries,
            Map<ElementId, DocumentEntry> byId,
            Map<DocumentPath, DocumentEntry> byPath
    ) {
        this.entries = List.copyOf(entries);
        this.byId = Collections.unmodifiableMap(new LinkedHashMap<>(byId));
        this.byPath = Collections.unmodifiableMap(new LinkedHashMap<>(byPath));
    }

    public static DocumentIndex build(ApiDocument document) {
        DocumentStructureValidator.validate(document);
        List<DocumentEntry> entries = new ArrayList<>();
        Map<ElementId, DocumentEntry> byId = new LinkedHashMap<>();
        Map<DocumentPath, DocumentEntry> byPath = new LinkedHashMap<>();
        new ModelWalker().walkDocument(document, (value, path) -> {
            DocumentEntry entry = new DocumentEntry(path, value);
            entries.add(entry);
            byPath.put(path, entry);
            if (value instanceof IdentifiedElement element) {
                byId.put(element.getId(), entry);
            }
        });
        return new DocumentIndex(entries, byId, byPath);
    }

    public List<DocumentEntry> entries() {
        return entries;
    }

    public Optional<DocumentEntry> findById(ElementId id) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id")));
    }

    public Optional<DocumentEntry> findByPath(DocumentPath path) {
        return Optional.ofNullable(byPath.get(Objects.requireNonNull(path, "path")));
    }
}
