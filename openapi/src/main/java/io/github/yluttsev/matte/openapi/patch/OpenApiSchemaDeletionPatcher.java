package io.github.yluttsev.matte.openapi.patch;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceFormat;

public final class OpenApiSchemaDeletionPatcher {

    private static final Map<SourceFormat, SourceScalarScanner> SCANNERS = Map.of(
            SourceFormat.JSON,
            new JsonScalarScanner(),
            SourceFormat.YAML,
            new YamlScalarScanner()
    );
    private static final Map<SourceFormat, SchemaValueSourceEditor> VALUE_EDITORS = Map.of(
            SourceFormat.JSON,
            new JsonValueSourceEditor(),
            SourceFormat.YAML,
            new YamlValueSourceEditor()
    );

    public List<DocumentPath> incomingReferences(
            SourceDocument source,
            SourceFormat format,
            DocumentPath targetPath
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(targetPath, "targetPath");
        SchemaIdScopeDetector.rejectIfPresent(source, format);

        String text = decode(source);
        SchemaReferenceRelocator matcher = new SchemaReferenceRelocator(targetPath, targetPath);
        List<SourceScalar> scalars = SCANNERS.get(format).scan(text);
        return Stream.concat(
                scalars.stream().filter(scalar -> matcher.referencesTarget(scalar, targetPath)).map(SourceScalar::path),
                matcher.unrecognizedReferences(scalars).stream()
        )
                .filter(path -> !path.startsWith(targetPath))
                .distinct()
                .toList();
    }

    public SourceDocument remove(
            SourceDocument source,
            SourceFormat format,
            DocumentPath path
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(path, "path");
        SchemaIdScopeDetector.rejectIfPresent(source, format);

        String text = decode(source);
        String updated = VALUE_EDITORS.get(format).remove(text, path);
        if (updated.equals(text)) {
            return source;
        }
        return new SourceDocument(updated.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(SourceDocument source) {
        try {
            return source.decodeUtf8();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Source is not valid UTF-8", exception);
        }
    }
}
