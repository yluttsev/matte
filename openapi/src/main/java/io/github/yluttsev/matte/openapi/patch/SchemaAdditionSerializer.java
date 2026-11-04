package io.github.yluttsev.matte.openapi.patch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.OpenApiVersion;

public final class SchemaAdditionSerializer {

    public Map<String, Object> serialize(Schema schema, OpenApiVersion version) {
        if (!(schema instanceof SchemaObject object)) {
            throw new IllegalArgumentException("Only object schemas can be added to the source");
        }
        requireSupportedFields(object);
        Map<String, Object> result = new LinkedHashMap<>();
        addTypes(result, object.getTypes(), version);
        addIfPresent(result, "title", object.getTitle());
        addIfPresent(result, "description", object.getDescription());
        addIfPresent(result, "format", object.getFormat());
        if (object.getReference() != null) {
            result.put("$ref", object.getReference().value());
        }
        if (!object.getRequired().isEmpty()) {
            result.put("required", new ArrayList<>(object.getRequired()));
        }
        if (!object.getProperties().isEmpty()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (SchemaProperty property : object.getProperties()) {
                properties.put(property.getName(), serialize(property.getSchema(), version));
            }
            result.put("properties", properties);
        }
        return result;
    }

    private void addTypes(Map<String, Object> result, Set<JsonType> declared, OpenApiVersion version) {
        Set<JsonType> types = new LinkedHashSet<>(declared);
        if (version == OpenApiVersion.V3_0) {
            boolean nullable = types.remove(JsonType.NULL);
            if (types.size() > 1 || nullable && types.isEmpty()) {
                throw new IllegalArgumentException("OpenAPI 3.0 requires one non-null type for nullable schemas");
            }
            if (!types.isEmpty()) {
                result.put("type", typeName(types.iterator().next()));
            }
            if (nullable) {
                result.put("nullable", true);
            }
        } else if (!types.isEmpty()) {
            List<String> names = types.stream().map(this::typeName).toList();
            result.put("type", names.size() == 1 ? names.getFirst() : names);
        }
    }

    private String typeName(JsonType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private void addIfPresent(Map<String, Object> result, String key, String value) {
        if (value != null) {
            result.put(key, value);
        }
    }

    private void requireSupportedFields(SchemaObject schema) {
        if (hasUnsupportedValues(schema) || hasUnsupportedConstraints(schema) || hasUnsupportedCombinations(schema)) {
            throw new IllegalArgumentException("Schema contains fields unsupported by the OpenAPI importer");
        }
    }

    private boolean hasUnsupportedValues(SchemaObject schema) {
        return schema.getDefaultValue() != null || schema.getExample() != null
                || !schema.getExamples().isEmpty() || !schema.getEnumValues().isEmpty()
                || schema.getConstValue() != null || schema.getDeprecated() != null
                || schema.getReadOnly() != null || schema.getWriteOnly() != null;
    }

    private boolean hasUnsupportedConstraints(SchemaObject schema) {
        return schema.getAdditionalProperties() != null || schema.getMinProperties() != null
                || schema.getMaxProperties() != null || schema.getItems() != null
                || schema.getMinItems() != null || schema.getMaxItems() != null
                || schema.getUniqueItems() != null || schema.getMinLength() != null
                || schema.getMaxLength() != null || schema.getPattern() != null
                || schema.getMinimum() != null || schema.getMaximum() != null
                || schema.getExclusiveMinimum() != null || schema.getExclusiveMaximum() != null
                || schema.getMultipleOf() != null;
    }

    private boolean hasUnsupportedCombinations(SchemaObject schema) {
        return !schema.getAllOf().isEmpty() || !schema.getOneOf().isEmpty() || !schema.getAnyOf().isEmpty()
                || schema.getNot() != null || schema.getDiscriminator() != null;
    }
}
