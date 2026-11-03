package io.github.yluttsev.matte.openapi.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

public final class CoreModelMapper {

    public ApiDocument map(OpenAPI openApi, OpenApiVersion version) {
        Objects.requireNonNull(openApi, "openApi");
        Objects.requireNonNull(version, "version");
        ApiDocument.Builder document = ApiDocument.builder(ElementId.random());
        if (openApi.getInfo() != null) {
            document.info(mapInfo(openApi.getInfo()));
        }
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            document.components(mapComponents(openApi.getComponents().getSchemas(), version));
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
            OpenApiVersion version
    ) {
        List<SchemaComponent> schemas = new ArrayList<>();
        for (var entry : sourceSchemas.entrySet()) {
            schemas.add(new SchemaComponent(
                    ElementId.random(),
                    entry.getKey(),
                    mapSchema(entry.getValue(), Collections.newSetFromMap(new IdentityHashMap<>()), version)
            ));
        }
        return Components.builder().schemas(schemas).build();
    }

    private Schema mapSchema(
            io.swagger.v3.oas.models.media.Schema<?> source,
            Set<io.swagger.v3.oas.models.media.Schema<?>> ancestors,
            OpenApiVersion version
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
            return SchemaObject.builder(ElementId.random())
                    .types(extractTypes(source, version))
                    .title(source.getTitle())
                    .description(source.getDescription())
                    .format(source.getFormat())
                    .reference(source.get$ref() == null ? null : new SchemaReference(source.get$ref()))
                    .properties(mapProperties(source.getProperties(), ancestors, version))
                    .required(source.getRequired() == null ? Set.of() : new LinkedHashSet<>(source.getRequired()))
                    .build();
        } finally {
            ancestors.remove(source);
        }
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
            OpenApiVersion version
    ) {
        if (sourceProperties == null) {
            return List.of();
        }
        List<SchemaProperty> properties = new ArrayList<>();
        for (var entry : sourceProperties.entrySet()) {
            properties.add(new SchemaProperty(
                    ElementId.random(),
                    entry.getKey(),
                    mapSchema(entry.getValue(), ancestors, version)
            ));
        }
        return properties;
    }
}
