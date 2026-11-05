package io.github.yluttsev.matte.openapi.patch;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ChangeSet;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentAdded;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyAdded;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.ImportResult;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceFormat;

public final class OpenApiAdditionPatcher {

    private static final Map<SourceFormat, SourceAdditionWriter> WRITERS = Map.of(
            SourceFormat.JSON,
            new JsonAdditionWriter(),
            SourceFormat.YAML,
            new YamlAdditionWriter()
    );

    public SourceDocument apply(ImportResult imported, ChangeSet changes) {
        Objects.requireNonNull(imported, "imported");
        Objects.requireNonNull(changes, "changes");
        if (changes.changes().size() != 1) {
            throw new IllegalArgumentException("Expected exactly one schema addition");
        }
        Change change = changes.changes().getFirst();
        Addition addition = addition(imported, change);
        OpenApiVersion version = OpenApiVersion.supported(imported.version().orElseThrow()).orElseThrow();
        Object schema = new SchemaAdditionSerializer().serialize(addition.schema(), version);
        SourceFormat format = imported.format().orElseThrow();
        String text = decode(imported.source());
        String updated = WRITERS.get(format).add(
                text,
                addition.parent(),
                addition.requiredAncestor(),
                addition.name(),
                schema
        );
        return new SourceDocument(updated.getBytes(StandardCharsets.UTF_8));
    }

    private Addition addition(ImportResult imported, Change change) {
        if (change instanceof SchemaComponentAdded component) {
            DocumentPath parent = DocumentPath.schemaComponent(component.name()).parent().orElseThrow();
            return new Addition(parent, DocumentPath.root(), component.name(), component.schema());
        }
        if (change instanceof SchemaPropertyAdded property) {
            DocumentPath schema = DocumentIndex.build(imported.document().orElseThrow())
                    .findById(property.schemaId())
                    .orElseThrow(() -> new IllegalArgumentException("Schema target was not found"))
                    .path();
            return new Addition(schema.child("properties"), schema, property.name(), property.schema());
        }
        throw new IllegalArgumentException("Unsupported source change: " + change.getClass().getSimpleName());
    }

    private String decode(SourceDocument source) {
        try {
            return source.decodeUtf8();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Source is not valid UTF-8", exception);
        }
    }

    private record Addition(DocumentPath parent, DocumentPath requiredAncestor, String name, Schema schema) {
    }
}
