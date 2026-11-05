package io.github.yluttsev.matte.openapi.patch;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ChangeSet;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentRenamed;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceFormat;

public final class OpenApiSourcePatcher {

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
        SchemaComponentRenamed rename = renameChange(changes);
        String text = decode(source);
        List<SourceScalar> scalars = SCANNERS.get(format).scan(text);
        SchemaReferenceRelocator relocator = new SchemaReferenceRelocator(rename.oldName(), rename.newName());
        validateChanges(changes, scalars, relocator);
        SourceTextEdits edits = edits(scalars, rename, relocator);
        return new SourceDocument(edits.apply(text).getBytes(StandardCharsets.UTF_8));
    }

    private SchemaComponentRenamed renameChange(ChangeSet changes) {
        SchemaComponentRenamed rename = null;
        for (Change change : changes.changes()) {
            if (change instanceof SchemaComponentRenamed component) {
                if (rename != null) {
                    throw new IllegalArgumentException("Only one schema rename can be patched at a time");
                }
                rename = component;
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
            List<SourceScalar> scalars,
            SchemaComponentRenamed rename,
            SchemaReferenceRelocator relocator
    ) {
        SourceTextEdits edits = new SourceTextEdits();
        boolean found = false;
        for (SourceScalar scalar : scalars) {
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
            throw new IllegalArgumentException("Schema component was not found in source: " + rename.oldName());
        }
        return edits;
    }

    private String decode(SourceDocument source) {
        try {
            return source.decodeUtf8();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Source is not valid UTF-8", exception);
        }
    }
}
