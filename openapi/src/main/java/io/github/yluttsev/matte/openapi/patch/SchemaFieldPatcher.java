package io.github.yluttsev.matte.openapi.patch;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.change.SchemaObjectUpdated;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceFormat;

public final class SchemaFieldPatcher {

    private static final Map<SourceFormat, SourceAdditionWriter> ADDITION_WRITERS = Map.of(
            SourceFormat.JSON, new JsonAdditionWriter(),
            SourceFormat.YAML, new YamlAdditionWriter()
    );
    private static final Map<SourceFormat, SchemaValueSourceEditor> VALUE_EDITORS = Map.of(
            SourceFormat.JSON, new JsonValueSourceEditor(),
            SourceFormat.YAML, new YamlValueSourceEditor()
    );

    public SourceDocument apply(
            SourceDocument source,
            SourceFormat format,
            OpenApiVersion version,
            SchemaObjectUpdated change
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(change, "change");

        SchemaAdditionSerializer serializer = new SchemaAdditionSerializer();
        Map<String, Object> before = serializer.serialize(change.before(), version);
        Map<String, Object> after = serializer.serialize(change.after(), version);
        String original = decode(source);
        String text = original;
        for (String field : fields(before, after)) {
            text = updateField(text, change.path(), field, before, after, format);
        }
        if (text.equals(original)) {
            return source;
        }
        return new SourceDocument(text.getBytes(StandardCharsets.UTF_8));
    }

    private String updateField(
            String text,
            DocumentPath schemaPath,
            String field,
            Map<String, Object> before,
            Map<String, Object> after,
            SourceFormat format
    ) {
        boolean wasPresent = before.containsKey(field);
        boolean isPresent = after.containsKey(field);
        if (wasPresent && isPresent && Objects.equals(before.get(field), after.get(field))) {
            return text;
        }
        if (!wasPresent) {
            return addField(text, schemaPath, field, after.get(field), format);
        }
        if (!isPresent) {
            return VALUE_EDITORS.get(format).remove(text, schemaPath.child(field));
        }
        return VALUE_EDITORS.get(format).replace(text, schemaPath.child(field), after.get(field));
    }

    private String addField(String text, DocumentPath schemaPath, String field, Object value, SourceFormat format) {
        return ADDITION_WRITERS.get(format).add(text, schemaPath, schemaPath, field, value);
    }

    private Set<String> fields(Map<String, Object> before, Map<String, Object> after) {
        Set<String> fields = new LinkedHashSet<>(before.keySet());
        fields.addAll(after.keySet());
        return fields;
    }

    private String decode(SourceDocument source) {
        try {
            return source.decodeUtf8();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Source is not valid UTF-8", exception);
        }
    }
}
