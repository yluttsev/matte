package io.github.yluttsev.matte.openapi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.value.NumberValue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaModelConsistencyVerifierTest {

    private final SchemaModelConsistencyVerifier verifier = new SchemaModelConsistencyVerifier();

    @Test
    void testVerify_WhenSchemaIdsDifferButValuesAndReferencesMatch_ShouldSucceed() {
        ApiDocument semanticResult = document("1.00", "https://example.com/target", "nested");
        ApiDocument reimported = document("1.0", "https://example.com/target", "nested");

        assertDoesNotThrow(() -> verifier.verify(semanticResult, reimported, OpenApiVersion.V3_1));
    }

    @Test
    void testVerify_WhenNestedSchemaValueDiffers_ShouldRejectMismatch() {
        ApiDocument semanticResult = document("1.0", "https://example.com/target", "nested");
        ApiDocument reimported = document("1.0", "https://example.com/target", "changed");

        assertThrows(
                IllegalArgumentException.class,
                () -> verifier.verify(semanticResult, reimported, OpenApiVersion.V3_1)
        );
    }

    @Test
    void testVerify_WhenSchemaReferenceDiffers_ShouldRejectMismatch() {
        ApiDocument semanticResult = document("1.0", "https://example.com/expected", "nested");
        ApiDocument reimported = document("1.0", "https://example.com/actual", "nested");

        assertThrows(
                IllegalArgumentException.class,
                () -> verifier.verify(semanticResult, reimported, OpenApiVersion.V3_1)
        );
    }

    @Test
    void testVerify_WhenSchemaValueDiffers_ShouldRejectMismatch() {
        ApiDocument semanticResult = document("1.0", "https://example.com/target", "nested");
        ApiDocument reimported = document("2.0", "https://example.com/target", "nested");

        assertThrows(
                IllegalArgumentException.class,
                () -> verifier.verify(semanticResult, reimported, OpenApiVersion.V3_1)
        );
    }

    private ApiDocument document(String defaultValue, String reference, String nestedTitle) {
        SchemaObject nestedSchema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.STRING))
                .title(nestedTitle)
                .build();
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .reference(new SchemaReference(reference))
                .defaultValue(new NumberValue(new BigDecimal(defaultValue)))
                .properties(List.of(new SchemaProperty(ElementId.random(), "child", nestedSchema)))
                .build();
        return ApiDocument.builder(ElementId.random())
                .components(Components.builder()
                        .schemas(List.of(new SchemaComponent(ElementId.random(), "Item", schema)))
                        .build())
                .build();
    }
}
