package io.github.yluttsev.matte.openapi.patch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SchemaComparison {

    private static final String REQUIRED_FIELD = "required";
    private static final Set<String> SCHEMA_MAP_FIELDS = Set.of("properties");
    private static final Set<String> SCHEMA_FIELDS = Set.of("items", "additionalProperties", "not");
    private static final Set<String> SCHEMA_LIST_FIELDS = Set.of("allOf", "oneOf", "anyOf");

    private SchemaComparison() {
    }

    public static Object normalizeSchema(Object schema) {
        if (schema instanceof Map<?, ?> mapping) {
            Map<Object, Object> normalized = new LinkedHashMap<>();
            mapping.forEach((key, value) -> normalized.put(key, normalizeField(key, value)));
            return normalized;
        }
        return normalizeValue(schema);
    }

    public static Object normalizeField(Object field, Object value) {
        if (REQUIRED_FIELD.equals(field) && value instanceof List<?> names) {
            Set<Object> normalized = new LinkedHashSet<>();
            names.forEach(name -> normalized.add(normalizeValue(name)));
            return normalized;
        }
        if (SCHEMA_MAP_FIELDS.contains(field) && value instanceof Map<?, ?> schemas) {
            Map<Object, Object> normalized = new LinkedHashMap<>();
            schemas.forEach((name, schema) -> normalized.put(name, normalizeSchema(schema)));
            return normalized;
        }
        if (SCHEMA_FIELDS.contains(field)) {
            return normalizeSchema(value);
        }
        if (SCHEMA_LIST_FIELDS.contains(field) && value instanceof List<?> schemas) {
            List<Object> normalized = new ArrayList<>();
            schemas.forEach(schema -> normalized.add(normalizeSchema(schema)));
            return normalized;
        }
        return normalizeValue(value);
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Number number) {
            return normalizeNumber(number);
        }
        if (value instanceof Map<?, ?> mapping) {
            Map<Object, Object> normalized = new LinkedHashMap<>();
            mapping.forEach((key, nested) -> normalized.put(key, normalizeValue(nested)));
            return normalized;
        }
        if (value instanceof List<?> sequence) {
            List<Object> normalized = new ArrayList<>();
            sequence.forEach(nested -> normalized.add(normalizeValue(nested)));
            return normalized;
        }
        return value;
    }

    private static Object normalizeNumber(Number number) {
        try {
            return new BigDecimal(number.toString()).stripTrailingZeros();
        } catch (NumberFormatException exception) {
            return number;
        }
    }
}
