package io.github.yluttsev.matte.openapi.patch;

import java.math.BigDecimal;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceFormat;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class SchemaSourceVerifier {

    private static final char UTF8_BOM = 0xFEFF;
    private static final LoadSettings YAML_SETTINGS = LoadSettings.builder().build();
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    public void verifyAddedSchema(
            SourceDocument source,
            SourceFormat format,
            DocumentPath path,
            Schema expected,
            OpenApiVersion version
    ) {
        Object expectedValue = new SchemaAdditionSerializer().serialize(expected, version);
        if (expectedValue instanceof Map<?, ?> mapping) {
            Map<String, Object> expectedFields = stringKeyedMap(mapping);
            verifyFields(source, format, path, expectedFields.keySet(), expectedFields);
            return;
        }
        verifyValue(source, format, path, expectedValue);
    }

    public void verifyUpdatedSchema(
            SourceDocument source,
            SourceFormat format,
            DocumentPath path,
            Schema before,
            Schema after,
            OpenApiVersion version
    ) {
        SchemaAdditionSerializer serializer = new SchemaAdditionSerializer();
        Map<String, Object> beforeFields = stringKeyedMap(serializer.serialize(before, version));
        Map<String, Object> afterFields = stringKeyedMap(serializer.serialize(after, version));
        Set<String> fields = changedFields(beforeFields, afterFields);
        verifyFields(source, format, path, fields, afterFields);
    }

    private Set<String> changedFields(Map<String, Object> before, Map<String, Object> after) {
        Set<String> fields = new LinkedHashSet<>(before.keySet());
        fields.addAll(after.keySet());
        fields.removeIf(
                field -> before.containsKey(field) && after.containsKey(field)
                && Objects.equals(normalize(before.get(field)), normalize(after.get(field)))
        );
        return fields;
    }

    private void verifyFields(
            SourceDocument source,
            SourceFormat format,
            DocumentPath path,
            Set<String> fields,
            Map<String, Object> expected
    ) {
        Object document = readDocument(source, format);
        Object value = valueAt(document, path);
        if (!(value instanceof Map<?, ?> actual)) {
            throw new IllegalArgumentException("Schema source was not preserved: " + path);
        }
        for (String field : fields) {
            boolean expectedPresent = expected.containsKey(field);
            boolean actualPresent = actual.containsKey(field);
            if (expectedPresent != actualPresent
                    || expectedPresent && !Objects.equals(
                            normalize(expected.get(field)),
                            normalize(actual.get(field))
                    )) {
                throw new IllegalArgumentException("Schema field was not preserved: " + path.child(field));
            }
        }
    }

    private void verifyValue(SourceDocument source, SourceFormat format, DocumentPath path, Object expected) {
        Object actual = valueAt(readDocument(source, format), path);
        if (!Objects.equals(normalize(expected), normalize(actual))) {
            throw new IllegalArgumentException("Schema source was not preserved: " + path);
        }
    }

    private Map<String, Object> stringKeyedMap(Object value) {
        if (!(value instanceof Map<?, ?> mapping)) {
            throw new IllegalArgumentException("Schema object did not serialize as a mapping");
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        mapping.forEach((key, fieldValue) -> fields.put((String) key, fieldValue));
        return fields;
    }

    private Object readDocument(SourceDocument source, SourceFormat format) {
        String text = decode(source);
        return switch (format) {
            case JSON -> {
                yield JSON.readValue(text, Object.class);
            }
            case YAML -> {
                yield new Load(YAML_SETTINGS).loadFromString(text);
            }
        };
    }

    private Object valueAt(Object document, DocumentPath path) {
        Object value = document;
        for (String segment : path.segments()) {
            if (value instanceof Map<?, ?> mapping) {
                value = mapping.get(segment);
            } else if (value instanceof List<?> sequence) {
                value = sequence.get(Integer.parseInt(segment));
            } else {
                return null;
            }
            if (value == null) {
                return null;
            }
        }
        return value;
    }

    private Object normalize(Object value) {
        if (value instanceof Number number) {
            return new BigDecimal(number.toString()).stripTrailingZeros();
        }
        if (value instanceof Map<?, ?> mapping) {
            Map<Object, Object> normalized = new LinkedHashMap<>();
            mapping.forEach((key, nested) -> normalized.put(key, normalize(nested)));
            return normalized;
        }
        if (value instanceof List<?> sequence) {
            List<Object> normalized = new ArrayList<>();
            sequence.forEach(nested -> normalized.add(normalize(nested)));
            return normalized;
        }
        return value;
    }

    private String decode(SourceDocument source) {
        try {
            String text = source.decodeUtf8();
            if (!text.isEmpty() && text.charAt(0) == UTF8_BOM) {
                return text.substring(1);
            }
            return text;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Source is not valid UTF-8", exception);
        }
    }
}
