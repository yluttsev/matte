package io.github.yluttsev.matte.openapi.patch;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.change.SchemaObjectUpdated;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceFormat;

public final class SchemaFieldPatcher {

    private static final String REQUIRED_FIELD = "required";
    private static final Set<String> NESTED_SCHEMA_FIELDS = Set.of(
            "properties",
            "items",
            "additionalProperties",
            "allOf",
            "oneOf",
            "anyOf",
            "not",
            "discriminator"
    );
    private static final Map<SourceFormat, SourceAdditionWriter> ADDITION_WRITERS = Map.of(
            SourceFormat.JSON,
            new JsonAdditionWriter(),
            SourceFormat.YAML,
            new YamlAdditionWriter()
    );
    private static final Map<SourceFormat, SchemaValueSourceEditor> VALUE_EDITORS = Map.of(
            SourceFormat.JSON,
            new JsonValueSourceEditor(),
            SourceFormat.YAML,
            new YamlValueSourceEditor()
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
        rejectNestedReplacements(before, after);
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

    private void rejectNestedReplacements(Map<String, Object> before, Map<String, Object> after) {
        for (String field : NESTED_SCHEMA_FIELDS) {
            if (before.containsKey(field) && !Objects.equals(before.get(field), after.get(field))) {
                throw new IllegalArgumentException("Cannot safely replace nested schema field: " + field);
            }
        }
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
        if (wasPresent && isPresent && Objects.equals(
                SchemaComparison.normalizeField(field, before.get(field)),
                SchemaComparison.normalizeField(field, after.get(field))
        )) {
            return text;
        }
        if (wasPresent && isPresent && REQUIRED_FIELD.equals(field)) {
            return updateRequired(text, schemaPath, before.get(field), after.get(field), format);
        }
        if (!wasPresent) {
            return addField(text, schemaPath, field, after.get(field), format);
        }
        if (!isPresent) {
            return VALUE_EDITORS.get(format).remove(text, schemaPath.child(field));
        }
        return VALUE_EDITORS.get(format).replace(text, schemaPath.child(field), after.get(field));
    }

    private String updateRequired(
            String text,
            DocumentPath schemaPath,
            Object before,
            Object after,
            SourceFormat format
    ) {
        Set<String> previous = names(before);
        Set<String> next = names(after);
        Set<String> removed = new LinkedHashSet<>(previous);
        removed.removeAll(next);
        Set<String> added = new LinkedHashSet<>(next);
        added.removeAll(previous);
        return new RequiredArrayItemPatcher().applyDifference(
                text,
                format,
                schemaPath.child(REQUIRED_FIELD),
                removed,
                added
        );
    }

    private Set<String> names(Object value) {
        Set<String> names = new LinkedHashSet<>();
        for (Object name : (List<?>) value) {
            names.add((String) name);
        }
        return names;
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
