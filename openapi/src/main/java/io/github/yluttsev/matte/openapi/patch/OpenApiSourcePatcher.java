package io.github.yluttsev.matte.openapi.patch;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ChangeSet;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentRenamed;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyRenamed;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceEditCode;
import io.github.yluttsev.matte.openapi.SourceFormat;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.Node;

public final class OpenApiSourcePatcher {

    private static final String PROPERTIES_FIELD = "properties";
    private static final String REQUIRED_FIELD = "required";

    private static final Map<SourceFormat, SourceScalarScanner> SCANNERS = Map.of(
            SourceFormat.JSON,
            new JsonScalarScanner(),
            SourceFormat.YAML,
            new YamlScalarScanner()
    );

    public SourceDocument apply(SourceDocument source, SourceFormat format, ChangeSet changes) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(changes, "changes");
        if (changes.changes().isEmpty()) {
            return source;
        }
        if (changes.changes().stream().anyMatch(this::isSchemaRename)) {
            SchemaIdScopeDetector.rejectIfPresent(source, format);
        }
        Rename rename = renameChange(changes);
        String text = decode(source);
        List<SourceScalar> scalars = SCANNERS.get(format).scan(text);
        SchemaReferenceRelocator relocator = rename.relocator();
        List<DocumentPath> unrecognized = relocator.unrecognizedReferences(scalars);
        if (!unrecognized.isEmpty()) {
            throw new SourceEditRefusal(
                    SourceEditCode.UNRECOGNIZED_REFERENCE,
                    "Unrecognized reference locations: " + unrecognized,
                    unrecognized.getFirst()
            );
        }
        requireUnsharedReferences(text, format, changes);
        validateChanges(changes, scalars, relocator);
        SourceTextEdits edits = edits(text, format, scalars, rename, relocator);
        return new SourceDocument(edits.apply(text).getBytes(StandardCharsets.UTF_8));
    }

    private boolean isSchemaRename(Change change) {
        return change instanceof SchemaComponentRenamed || change instanceof SchemaPropertyRenamed;
    }

    private void requireUnsharedReferences(String text, SourceFormat format, ChangeSet changes) {
        if (format != SourceFormat.YAML) {
            return;
        }
        List<ReferenceChanged> references = changes.changes().stream()
                .filter(ReferenceChanged.class::isInstance)
                .map(ReferenceChanged.class::cast)
                .toList();
        if (references.isEmpty()) {
            return;
        }
        Node root = new Compose(LoadSettings.builder().build()).composeString(text).orElseThrow();
        for (ReferenceChanged reference : references) {
            YamlAliasSafety.requireUniqueChangedValue(root, reference.sourceBefore());
        }
    }

    private Rename renameChange(ChangeSet changes) {
        Rename rename = null;
        for (Change change : changes.changes()) {
            if (change instanceof SchemaComponentRenamed component) {
                if (rename != null) {
                    throw new IllegalArgumentException("Only one schema rename can be patched at a time");
                }
                rename = new Rename(
                        component.oldName(),
                        component.newName(),
                        DocumentPath.schemaComponent(component.oldName()),
                        DocumentPath.schemaComponent(component.newName()),
                        null
                );
            } else if (change instanceof SchemaPropertyRenamed property) {
                if (rename != null) {
                    throw new IllegalArgumentException("Only one schema rename can be patched at a time");
                }
                DocumentPath oldPath = property.schemaPath().child(PROPERTIES_FIELD).child(property.oldName());
                DocumentPath newPath = property.schemaPath().child(PROPERTIES_FIELD).child(property.newName());
                rename = new Rename(
                        property.oldName(),
                        property.newName(),
                        oldPath,
                        newPath,
                        property.schemaPath().child(REQUIRED_FIELD)
                );
            } else if (!(change instanceof ReferenceChanged)) {
                throw new IllegalArgumentException("Unsupported source change: " + change.getClass().getSimpleName());
            }
        }
        if (rename == null) {
            throw new IllegalArgumentException("Schema rename is missing");
        }
        return rename;
    }

    private void validateChanges(
            ChangeSet changes,
            List<SourceScalar> scalars,
            SchemaReferenceRelocator relocator
    ) {
        for (Change change : changes.changes()) {
            if (change instanceof ReferenceChanged reference) {
                String missingReference = "Reference was not found in source: " + reference.sourceBefore();
                SourceScalar scalar = scalars.stream()
                        .filter(item -> matchesReference(item, reference))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(missingReference));
                if (!relocator.updatedValue(scalar).equals(reference.newValue())) {
                    throw new IllegalArgumentException(
                            "Reference update differs from model: " + reference.sourceBefore()
                    );
                }
            }
        }
    }

    private boolean matchesReference(SourceScalar scalar, ReferenceChanged reference) {
        return !scalar.key() && scalar.path().equals(reference.sourceBefore())
                && scalar.value().equals(reference.oldValue());
    }

    private SourceTextEdits edits(
            String text,
            SourceFormat format,
            List<SourceScalar> scalars,
            Rename rename,
            SchemaReferenceRelocator relocator
    ) {
        SourceTextEdits edits = new SourceTextEdits();
        Set<DocumentPath> removedRequiredItems = removeDuplicateRequiredItems(text, format, scalars, rename, edits);
        boolean found = false;
        for (SourceScalar scalar : scalars) {
            if (removedRequiredItems.contains(scalar.path())) {
                continue;
            }
            if (scalar.key() && scalar.path().equals(relocator.oldPath())) {
                found = true;
                edits.replace(scalar, rename.newName());
            } else if (!scalar.key()) {
                String updated = relocator.updatedValue(scalar);
                if (!updated.equals(scalar.value())) {
                    edits.replace(scalar, updated);
                }
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Schema element was not found in source: " + rename.oldName());
        }
        return edits;
    }

    private Set<DocumentPath> removeDuplicateRequiredItems(
            String text,
            SourceFormat format,
            List<SourceScalar> scalars,
            Rename rename,
            SourceTextEdits edits
    ) {
        if (rename.requiredPath() == null || rename.oldName().equals(rename.newName())) {
            return Set.of();
        }
        List<SourceScalar> requiredItems = scalars.stream()
                .filter(scalar -> !scalar.key())
                .filter(scalar -> scalar.path().parent().filter(rename.requiredPath()::equals).isPresent())
                .sorted(Comparator.comparingInt(this::sequenceIndex))
                .toList();
        boolean newNameAlreadyExists = requiredItems.stream()
                .anyMatch(item -> item.value().equals(rename.newName()));
        if (!newNameAlreadyExists) {
            return Set.of();
        }
        List<SourceScalar> oldItems = requiredItems.stream()
                .filter(item -> item.value().equals(rename.oldName()))
                .toList();
        if (oldItems.isEmpty()) {
            return Set.of();
        }
        return new RequiredArrayItemPatcher().remove(
                text,
                format,
                rename.requiredPath(),
                requiredItems,
                oldItems,
                edits
        );
    }

    private int sequenceIndex(SourceScalar scalar) {
        String segment = scalar.path().segments().getLast();
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Required entry does not have a sequence index: " + scalar.path(),
                    exception
            );
        }
    }

    private String decode(SourceDocument source) {
        try {
            return source.decodeUtf8();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Source is not valid UTF-8", exception);
        }
    }

    private record Rename(
            String oldName,
            String newName,
            DocumentPath oldPath,
            DocumentPath newPath,
            DocumentPath requiredPath
    ) {

        private SchemaReferenceRelocator relocator() {
            if (requiredPath == null) {
                return new SchemaReferenceRelocator(oldName, newName);
            }
            return new SchemaReferenceRelocator(
                    oldPath,
                    newPath,
                    requiredPath,
                    oldName,
                    newName
            );
        }
    }
}
