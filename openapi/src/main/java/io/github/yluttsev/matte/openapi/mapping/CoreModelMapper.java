package io.github.yluttsev.matte.openapi.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.info.ApiInfo;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.value.ArrayValue;
import io.github.yluttsev.matte.core.model.value.BooleanValue;
import io.github.yluttsev.matte.core.model.value.DocumentValue;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.NumberValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

public final class CoreModelMapper {

    public ApiDocument map(OpenAPI openApi, OpenApiVersion version) {
        return map(openApi, version, Map.of());
    }

    public ApiDocument map(
            OpenAPI openApi,
            OpenApiVersion version,
            Map<DocumentPath, SchemaValueFields> schemaValues
    ) {
        Objects.requireNonNull(openApi, "openApi");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(schemaValues, "schemaValues");
        ApiDocument.Builder document = ApiDocument.builder(ElementId.random());
        if (openApi.getInfo() != null) {
            document.info(mapInfo(openApi.getInfo()));
        }
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            document.components(mapComponents(openApi.getComponents().getSchemas(), version, schemaValues));
        }
        return document.build();
    }

    private ApiInfo mapInfo(Info info) {
        return ApiInfo.builder()
                .title(info.getTitle())
                .summary(info.getSummary())
                .description(info.getDescription())
                .version(info.getVersion())
                .termsOfService(info.getTermsOfService())
                .build();
    }

    private Components mapComponents(
            Map<String, io.swagger.v3.oas.models.media.Schema> sourceSchemas,
            OpenApiVersion version,
            Map<DocumentPath, SchemaValueFields> schemaValues
    ) {
        List<SchemaComponent> schemas = new ArrayList<>();
        for (var entry : sourceSchemas.entrySet()) {
            Schema schema = mapSchema(
                    entry.getValue(),
                    Collections.newSetFromMap(new IdentityHashMap<>()),
                    version,
                    DocumentPath.schemaComponent(entry.getKey()),
                    schemaValues
            );
            schemas.add(
                    new SchemaComponent(
                            ElementId.random(),
                            entry.getKey(),
                            schema
                    )
            );
        }
        return Components.builder().schemas(schemas).build();
    }

    private Schema mapSchema(
            io.swagger.v3.oas.models.media.Schema<?> source,
            Set<io.swagger.v3.oas.models.media.Schema<?>> ancestors,
            OpenApiVersion version,
            DocumentPath path,
            Map<DocumentPath, SchemaValueFields> schemaValues
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Schema definition is missing");
        }
        if (!ancestors.add(source)) {
            throw new IllegalArgumentException("Cyclic inline schema cannot be mapped");
        }
        try {
            if (source.getBooleanSchemaValue() != null) {
                return new BooleanSchema(ElementId.random(), source.getBooleanSchemaValue());
            }
            SchemaValueFields values = schemaValues.get(path);
            SchemaReference reference = null;
            if (source.get$ref() != null) {
                reference = new SchemaReference(source.get$ref());
            }
            DocumentValue defaultValue;
            DocumentValue example;
            List<DocumentValue> enumValues;
            if (values != null) {
                defaultValue = null;
                example = null;
                if (values.hasDefault()) {
                    defaultValue = values.defaultValue();
                }
                if (values.hasExample()) {
                    example = values.example();
                }
                enumValues = values.enumValues();
            } else {
                defaultValue = mapOptionalValue(source.getDefault(), source.getDefaultSetFlag());
                example = mapOptionalValue(source.getExample(), source.getExampleSetFlag());
                enumValues = mapValues(source.getEnum());
            }
            Set<String> required = mapRequired(source.getRequired(), values);
            return SchemaObject.builder(ElementId.random())
                    .types(extractTypes(source, version))
                    .title(source.getTitle())
                    .description(source.getDescription())
                    .format(source.getFormat())
                    .reference(reference)
                    .defaultValue(defaultValue)
                    .example(example)
                    .examples(mapExamples(source, values, version))
                    .enumValues(enumValues)
                    .constValue(mapConst(source, values, version))
                    .deprecated(source.getDeprecated())
                    .readOnly(source.getReadOnly())
                    .writeOnly(source.getWriteOnly())
                    .properties(mapProperties(source.getProperties(), ancestors, version, path, schemaValues))
                    .required(required)
                    .items(
                    mapOptionalSchema(
                            source.getItems(),
                            ancestors,
                            version,
                            path.child("items"),
                            schemaValues
                    )
            )
                    .additionalProperties(
                    mapAdditionalProperties(
                            source.getAdditionalProperties(),
                            ancestors,
                            version,
                            path.child("additionalProperties"),
                            schemaValues
                    )
            )
                    .minProperties(integer(values, "minProperties", source.getMinProperties()))
                    .maxProperties(integer(values, "maxProperties", source.getMaxProperties()))
                    .minItems(integer(values, "minItems", source.getMinItems()))
                    .maxItems(integer(values, "maxItems", source.getMaxItems()))
                    .uniqueItems(source.getUniqueItems())
                    .minLength(integer(values, "minLength", source.getMinLength()))
                    .maxLength(integer(values, "maxLength", source.getMaxLength()))
                    .pattern(source.getPattern())
                    .minimum(source.getMinimum())
                    .maximum(source.getMaximum())
                    .exclusiveMinimum(exclusiveMinimum(source, version))
                    .exclusiveMaximum(exclusiveMaximum(source, version))
                    .multipleOf(source.getMultipleOf())
                    .allOf(mapSchemas(source.getAllOf(), ancestors, version, path.child("allOf"), schemaValues))
                    .oneOf(mapSchemas(source.getOneOf(), ancestors, version, path.child("oneOf"), schemaValues))
                    .anyOf(mapSchemas(source.getAnyOf(), ancestors, version, path.child("anyOf"), schemaValues))
                    .not(mapOptionalSchema(source.getNot(), ancestors, version, path.child("not"), schemaValues))
                    .discriminator(mapDiscriminator(source.getDiscriminator()))
                    .build();
        } finally {
            ancestors.remove(source);
        }
    }

    private Set<String> mapRequired(List<String> parsed, SchemaValueFields values) {
        Set<String> required = new LinkedHashSet<>();
        if (parsed == null) {
            return required;
        }
        Set<String> parsedNames = new LinkedHashSet<>(parsed);
        if (values != null) {
            for (String name : values.requiredNames()) {
                if (parsedNames.contains(name)) {
                    required.add(name);
                }
            }
        }
        required.addAll(parsedNames);
        return required;
    }

    private Set<JsonType> extractTypes(io.swagger.v3.oas.models.media.Schema<?> source, OpenApiVersion version) {
        Set<JsonType> types = new LinkedHashSet<>();
        if (source.getTypes() != null) {
            for (String type : source.getTypes()) {
                types.add(parseJsonType(type));
            }
        } else if (source.getType() != null) {
            types.add(parseJsonType(source.getType()));
        }
        if (version == OpenApiVersion.V3_0 && !types.isEmpty() && Boolean.TRUE.equals(source.getNullable())) {
            types.add(JsonType.NULL);
        }
        return types;
    }

    private JsonType parseJsonType(String type) {
        try {
            return JsonType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported schema type: " + type, exception);
        }
    }

    private List<SchemaProperty> mapProperties(
            Map<String, io.swagger.v3.oas.models.media.Schema> sourceProperties,
            Set<io.swagger.v3.oas.models.media.Schema<?>> ancestors,
            OpenApiVersion version,
            DocumentPath path,
            Map<DocumentPath, SchemaValueFields> schemaValues
    ) {
        if (sourceProperties == null) {
            return List.of();
        }
        List<SchemaProperty> properties = new ArrayList<>();
        for (var entry : sourceProperties.entrySet()) {
            Schema schema = mapSchema(
                    entry.getValue(),
                    ancestors,
                    version,
                    path.child("properties").child(entry.getKey()),
                    schemaValues
            );
            properties.add(
                    new SchemaProperty(
                            ElementId.random(),
                            entry.getKey(),
                            schema
                    )
            );
        }
        return properties;
    }

    private List<Schema> mapSchemas(
            List<io.swagger.v3.oas.models.media.Schema> sourceSchemas,
            Set<io.swagger.v3.oas.models.media.Schema<?>> ancestors,
            OpenApiVersion version,
            DocumentPath path,
            Map<DocumentPath, SchemaValueFields> schemaValues
    ) {
        if (sourceSchemas == null) {
            return List.of();
        }
        List<Schema> schemas = new ArrayList<>();
        for (int index = 0; index < sourceSchemas.size(); index++) {
            schemas.add(
                    mapSchema(
                            sourceSchemas.get(index),
                            ancestors,
                            version,
                            path.child(Integer.toString(index)),
                            schemaValues
                    )
            );
        }
        return schemas;
    }

    private Schema mapOptionalSchema(
            io.swagger.v3.oas.models.media.Schema<?> source,
            Set<io.swagger.v3.oas.models.media.Schema<?>> ancestors,
            OpenApiVersion version,
            DocumentPath path,
            Map<DocumentPath, SchemaValueFields> schemaValues
    ) {
        if (source == null) {
            return null;
        }
        return mapSchema(source, ancestors, version, path, schemaValues);
    }

    private Schema mapAdditionalProperties(
            Object source,
            Set<io.swagger.v3.oas.models.media.Schema<?>> ancestors,
            OpenApiVersion version,
            DocumentPath path,
            Map<DocumentPath, SchemaValueFields> schemaValues
    ) {
        if (source == null) {
            return null;
        }
        if (source instanceof Boolean value) {
            return new BooleanSchema(ElementId.random(), value);
        }
        if (source instanceof io.swagger.v3.oas.models.media.Schema<?> schema) {
            return mapSchema(schema, ancestors, version, path, schemaValues);
        }
        throw new IllegalArgumentException("Unsupported additionalProperties value: " + source.getClass());
    }

    private Discriminator mapDiscriminator(io.swagger.v3.oas.models.media.Discriminator source) {
        if (source == null) {
            return null;
        }
        Map<String, SchemaReference> mapping = new LinkedHashMap<>();
        if (source.getMapping() != null) {
            source.getMapping().forEach((name, reference) -> mapping.put(name, new SchemaReference(reference)));
        }
        return Discriminator.builder().propertyName(source.getPropertyName()).mapping(mapping).build();
    }

    private DocumentValue mapOptionalValue(Object source, boolean present) {
        if (present) {
            return mapValue(source);
        }
        return null;
    }

    private List<DocumentValue> mapValues(List<?> source) {
        if (source == null) {
            return List.of();
        }
        List<DocumentValue> values = new ArrayList<>();
        for (Object value : source) {
            values.add(mapValue(value));
        }
        return values;
    }

    private List<DocumentValue> mapExamples(
            io.swagger.v3.oas.models.media.Schema<?> source,
            SchemaValueFields values,
            OpenApiVersion version
    ) {
        if (version == OpenApiVersion.V3_0) {
            return List.of();
        }
        if (values == null) {
            return mapValues(source.getExamples());
        }
        return values.examples();
    }

    private DocumentValue mapConst(
            io.swagger.v3.oas.models.media.Schema<?> source,
            SchemaValueFields values,
            OpenApiVersion version
    ) {
        if (version == OpenApiVersion.V3_0) {
            return null;
        }
        if (values == null) {
            return mapOptionalValue(source.getConst(), source.getConst() != null);
        }
        if (values.hasConst()) {
            return values.constValue();
        }
        return null;
    }

    private DocumentValue mapValue(Object source) {
        if (source == null) {
            return new NullValue();
        }
        if (source instanceof Boolean value) {
            return new BooleanValue(value);
        }
        if (source instanceof Number value) {
            return new NumberValue(decimal(value));
        }
        if (source instanceof String value) {
            return new StringValue(value);
        }
        if (source instanceof List<?> values) {
            return new ArrayValue(mapValues(values));
        }
        if (source instanceof Map<?, ?> properties) {
            Map<String, DocumentValue> values = new LinkedHashMap<>();
            properties.forEach(
                    (name, value) -> {
                        if (!(name instanceof String stringName)) {
                            throw new IllegalArgumentException("Schema value object keys must be strings");
                        }
                        values.put(stringName, mapValue(value));
                    }
            );
            return new ObjectValue(values);
        }
        throw new IllegalArgumentException("Unsupported schema value: " + source.getClass());
    }

    private BigDecimal decimal(Number value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private BigInteger integer(SchemaValueFields values, String field, Integer value) {
        if (values != null && values.integerConstraints().containsKey(field)) {
            return values.integerConstraints().get(field);
        }
        if (value == null) {
            return null;
        }
        return BigInteger.valueOf(value.longValue());
    }

    private BigDecimal exclusiveMinimum(io.swagger.v3.oas.models.media.Schema<?> source, OpenApiVersion version) {
        if (version == OpenApiVersion.V3_1) {
            return source.getExclusiveMinimumValue();
        }
        if (Boolean.TRUE.equals(source.getExclusiveMinimum())) {
            return source.getMinimum();
        }
        return null;
    }

    private BigDecimal exclusiveMaximum(io.swagger.v3.oas.models.media.Schema<?> source, OpenApiVersion version) {
        if (version == OpenApiVersion.V3_1) {
            return source.getExclusiveMaximumValue();
        }
        if (Boolean.TRUE.equals(source.getExclusiveMaximum())) {
            return source.getMaximum();
        }
        return null;
    }
}
