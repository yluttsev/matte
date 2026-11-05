package io.github.yluttsev.matte.openapi.patch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.value.ArrayValue;
import io.github.yluttsev.matte.core.model.value.BooleanValue;
import io.github.yluttsev.matte.core.model.value.DocumentValue;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.NumberValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.openapi.OpenApiVersion;

public final class SchemaAdditionSerializer {

    public Object serialize(Schema schema, OpenApiVersion version) {
        if (schema instanceof BooleanSchema booleanSchema) {
            return booleanSchema.getValue();
        }
        if (!(schema instanceof SchemaObject object)) {
            throw new IllegalArgumentException("Unsupported schema type: " + schema.getClass().getSimpleName());
        }
        return serialize(object, version);
    }

    public Map<String, Object> serialize(SchemaObject schema, OpenApiVersion version) {
        requireSupportedFields(schema, version);
        return serializeObject(schema, version);
    }

    private Map<String, Object> serializeObject(SchemaObject schema, OpenApiVersion version) {
        Map<String, Object> result = new LinkedHashMap<>();
        addTypes(result, schema.getTypes(), version);
        addIfPresent(result, "title", schema.getTitle());
        addIfPresent(result, "description", schema.getDescription());
        addIfPresent(result, "format", schema.getFormat());
        addReference(result, schema);
        addValues(result, schema);
        addFlags(result, schema);
        addObjectFields(result, schema, version);
        addArrayFields(result, schema, version);
        addScalarConstraints(result, schema, version);
        addCompositions(result, schema, version);
        addDiscriminator(result, schema.getDiscriminator());
        return result;
    }

    private void addReference(Map<String, Object> result, SchemaObject schema) {
        if (schema.getReference() != null) {
            result.put("$ref", schema.getReference().value());
        }
    }

    private void addValues(Map<String, Object> result, SchemaObject schema) {
        addValue(result, "default", schema.getDefaultValue());
        addValue(result, "example", schema.getExample());
        addValue(result, "const", schema.getConstValue());
        addValueList(result, "examples", schema.getExamples());
        addValueList(result, "enum", schema.getEnumValues());
    }

    private void addFlags(Map<String, Object> result, SchemaObject schema) {
        addIfPresent(result, "deprecated", schema.getDeprecated());
        addIfPresent(result, "readOnly", schema.getReadOnly());
        addIfPresent(result, "writeOnly", schema.getWriteOnly());
    }

    private void addObjectFields(Map<String, Object> result, SchemaObject schema, OpenApiVersion version) {
        if (!schema.getRequired().isEmpty()) {
            result.put("required", new ArrayList<>(schema.getRequired()));
        }
        if (!schema.getProperties().isEmpty()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (SchemaProperty property : schema.getProperties()) {
                properties.put(property.getName(), serialize(property.getSchema(), version));
            }
            result.put("properties", properties);
        }
        addSchema(result, "additionalProperties", schema.getAdditionalProperties(), version);
        addIfPresent(result, "minProperties", schema.getMinProperties());
        addIfPresent(result, "maxProperties", schema.getMaxProperties());
    }

    private void addArrayFields(Map<String, Object> result, SchemaObject schema, OpenApiVersion version) {
        addSchema(result, "items", schema.getItems(), version);
        addIfPresent(result, "minItems", schema.getMinItems());
        addIfPresent(result, "maxItems", schema.getMaxItems());
        addIfPresent(result, "uniqueItems", schema.getUniqueItems());
    }

    private void addScalarConstraints(Map<String, Object> result, SchemaObject schema, OpenApiVersion version) {
        addIfPresent(result, "minLength", schema.getMinLength());
        addIfPresent(result, "maxLength", schema.getMaxLength());
        addIfPresent(result, "pattern", schema.getPattern());
        addNumericBounds(result, schema, version);
        addIfPresent(result, "multipleOf", schema.getMultipleOf());
    }

    private void addNumericBounds(Map<String, Object> result, SchemaObject schema, OpenApiVersion version) {
        if (version == OpenApiVersion.V3_0) {
            addExclusiveBound(result, "minimum", "exclusiveMinimum", schema.getMinimum(),
                    schema.getExclusiveMinimum());
            addExclusiveBound(result, "maximum", "exclusiveMaximum", schema.getMaximum(),
                    schema.getExclusiveMaximum());
            return;
        }
        addIfPresent(result, "minimum", schema.getMinimum());
        addIfPresent(result, "maximum", schema.getMaximum());
        addIfPresent(result, "exclusiveMinimum", schema.getExclusiveMinimum());
        addIfPresent(result, "exclusiveMaximum", schema.getExclusiveMaximum());
    }

    private void addExclusiveBound(
            Map<String, Object> result,
            String boundName,
            String exclusiveName,
            Object inclusiveValue,
            Object exclusiveValue
    ) {
        if (exclusiveValue == null) {
            addIfPresent(result, boundName, inclusiveValue);
            return;
        }
        if (inclusiveValue != null && !inclusiveValue.equals(exclusiveValue)) {
            throw new IllegalArgumentException("OpenAPI 3.0 cannot represent different inclusive and exclusive bounds");
        }
        result.put(boundName, exclusiveValue);
        result.put(exclusiveName, true);
    }

    private void addCompositions(Map<String, Object> result, SchemaObject schema, OpenApiVersion version) {
        addSchemaList(result, "allOf", schema.getAllOf(), version);
        addSchemaList(result, "oneOf", schema.getOneOf(), version);
        addSchemaList(result, "anyOf", schema.getAnyOf(), version);
        addSchema(result, "not", schema.getNot(), version);
    }

    private void addDiscriminator(Map<String, Object> result, Discriminator discriminator) {
        if (discriminator == null) {
            return;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        addIfPresent(value, "propertyName", discriminator.getPropertyName());
        if (!discriminator.getMapping().isEmpty()) {
            Map<String, String> mapping = new LinkedHashMap<>();
            discriminator.getMapping().forEach((name, reference) -> mapping.put(name, reference.value()));
            value.put("mapping", mapping);
        }
        result.put("discriminator", value);
    }

    private void addTypes(Map<String, Object> result, Set<JsonType> declared, OpenApiVersion version) {
        List<JsonType> types = declared.stream().sorted().toList();
        if (version == OpenApiVersion.V3_0) {
            boolean nullable = types.contains(JsonType.NULL);
            types = types.stream().filter(type -> type != JsonType.NULL).toList();
            if (types.size() > 1 || nullable && types.isEmpty()) {
                throw new IllegalArgumentException("OpenAPI 3.0 requires one non-null type for nullable schemas");
            }
            if (!types.isEmpty()) {
                result.put("type", typeName(types.iterator().next()));
            }
            if (nullable) {
                result.put("nullable", true);
            }
            return;
        }
        if (!types.isEmpty()) {
            List<String> names = types.stream().map(this::typeName).toList();
            result.put("type", names.size() == 1 ? names.getFirst() : names);
        }
    }

    private String typeName(JsonType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private void addSchema(Map<String, Object> result, String name, Schema schema, OpenApiVersion version) {
        if (schema != null) {
            result.put(name, serialize(schema, version));
        }
    }

    private void addSchemaList(Map<String, Object> result, String name, List<Schema> schemas, OpenApiVersion version) {
        if (!schemas.isEmpty()) {
            result.put(name, schemas.stream().map(schema -> serialize(schema, version)).toList());
        }
    }

    private void addValue(Map<String, Object> result, String name, DocumentValue value) {
        if (value != null) {
            result.put(name, serializeValue(value));
        }
    }

    private void addValueList(Map<String, Object> result, String name, List<DocumentValue> values) {
        if (!values.isEmpty()) {
            result.put(name, values.stream().map(this::serializeValue).toList());
        }
    }

    private Object serializeValue(DocumentValue value) {
        if (value instanceof NullValue) {
            return null;
        }
        if (value instanceof BooleanValue booleanValue) {
            return booleanValue.value();
        }
        if (value instanceof NumberValue numberValue) {
            return numberValue.value();
        }
        if (value instanceof StringValue stringValue) {
            return stringValue.value();
        }
        if (value instanceof ArrayValue arrayValue) {
            return arrayValue.values().stream().map(this::serializeValue).toList();
        }
        if (value instanceof ObjectValue objectValue) {
            Map<String, Object> properties = new LinkedHashMap<>();
            objectValue.properties().forEach((name, property) -> properties.put(name, serializeValue(property)));
            return properties;
        }
        throw new IllegalArgumentException("Unsupported document value: " + value.getClass().getSimpleName());
    }

    private void addIfPresent(Map<String, Object> result, String name, Object value) {
        if (value != null) {
            result.put(name, value);
        }
    }

    private void requireSupportedFields(SchemaObject schema, OpenApiVersion version) {
        if (version == OpenApiVersion.V3_0
                && (!schema.getExamples().isEmpty() || schema.getConstValue() != null)) {
            throw new IllegalArgumentException("Schema examples and const require OpenAPI 3.1");
        }
        requireSupportedChildren(schema, version);
    }

    private void requireSupportedChildren(SchemaObject schema, OpenApiVersion version) {
        if (schema.getItems() != null) {
            requireSupported(schema.getItems(), version);
        }
        if (schema.getAdditionalProperties() != null) {
            requireSupported(schema.getAdditionalProperties(), version);
        }
        for (SchemaProperty property : schema.getProperties()) {
            requireSupported(property.getSchema(), version);
        }
        schema.getAllOf().forEach(child -> requireSupported(child, version));
        schema.getOneOf().forEach(child -> requireSupported(child, version));
        schema.getAnyOf().forEach(child -> requireSupported(child, version));
        if (schema.getNot() != null) {
            requireSupported(schema.getNot(), version);
        }
    }

    private void requireSupported(Schema schema, OpenApiVersion version) {
        if (schema instanceof BooleanSchema) {
            return;
        }
        requireSupportedFields((SchemaObject) schema, version);
    }
}
