package io.github.yluttsev.matte.openapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.openapi.patch.SchemaAdditionSerializer;
import io.github.yluttsev.matte.openapi.patch.SchemaComparison;

final class SchemaModelConsistencyVerifier {

    private final SchemaAdditionSerializer serializer = new SchemaAdditionSerializer();

    void verify(ApiDocument semanticResult, ApiDocument reimported, OpenApiVersion version) {
        Map<String, Object> expectedSchemas = schemas(semanticResult, version);
        Map<String, Object> actualSchemas = schemas(reimported, version);
        if (!Objects.equals(normalizeSchemas(expectedSchemas), normalizeSchemas(actualSchemas))) {
            throw new IllegalStateException(
                    "Re-imported OpenAPI schema model differs from the semantic edit result"
            );
        }
    }

    private Map<String, Object> normalizeSchemas(Map<String, Object> schemas) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        schemas.forEach((name, schema) -> normalized.put(name, SchemaComparison.normalizeSchema(schema)));
        return normalized;
    }

    private Map<String, Object> schemas(ApiDocument document, OpenApiVersion version) {
        Map<String, Object> schemas = new LinkedHashMap<>();
        for (SchemaComponent component : document.getComponents().getSchemas()) {
            schemas.put(component.getName(), serializer.serialize(component.getSchema(), version));
        }
        return schemas;
    }

}
