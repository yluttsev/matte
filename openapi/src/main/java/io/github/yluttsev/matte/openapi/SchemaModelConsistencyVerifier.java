package io.github.yluttsev.matte.openapi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.openapi.patch.SchemaAdditionSerializer;

final class SchemaModelConsistencyVerifier {

    private final SchemaAdditionSerializer serializer = new SchemaAdditionSerializer();

    void verify(ApiDocument semanticResult, ApiDocument reimported, OpenApiVersion version) {
        Map<String, Object> expectedSchemas = schemas(semanticResult, version);
        Map<String, Object> actualSchemas = schemas(reimported, version);
        if (!Objects.equals(normalize(expectedSchemas), normalize(actualSchemas))) {
            throw new IllegalArgumentException(
                    "Re-imported OpenAPI schema model differs from the semantic edit result"
            );
        }
    }

    private Map<String, Object> schemas(ApiDocument document, OpenApiVersion version) {
        Map<String, Object> schemas = new LinkedHashMap<>();
        for (SchemaComponent component : document.getComponents().getSchemas()) {
            schemas.put(component.getName(), serializer.serialize(component.getSchema(), version));
        }
        return schemas;
    }

    private Object normalize(Object value) {
        if (value instanceof Number number) {
            return normalizeNumber(number);
        }
        if (value instanceof Map<?, ?> mapping) {
            return normalizeMap(mapping);
        }
        if (value instanceof List<?> sequence) {
            return normalizeList(sequence);
        }
        return value;
    }

    private Object normalizeNumber(Number number) {
        try {
            return new BigDecimal(number.toString()).stripTrailingZeros();
        } catch (NumberFormatException exception) {
            return number;
        }
    }

    private Map<Object, Object> normalizeMap(Map<?, ?> mapping) {
        Map<Object, Object> normalized = new LinkedHashMap<>();
        mapping.forEach((key, nested) -> normalized.put(key, normalize(nested)));
        return normalized;
    }

    private List<Object> normalizeList(List<?> sequence) {
        List<Object> normalized = new ArrayList<>();
        sequence.forEach(nested -> normalized.add(normalize(nested)));
        return normalized;
    }
}
