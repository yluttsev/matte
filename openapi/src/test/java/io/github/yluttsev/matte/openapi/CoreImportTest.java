package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreImportTest {

    private final OpenApiImporter importer = new OpenApiImporter();

    @Test
    void testImportDocument_WhenSchemaIsYamlOrJson_ShouldBuildEquivalentCoreSubset() throws IOException {
        byte[] yaml = TestFixtures.bytes("schema-import.yaml");
        byte[] json = TestFixtures.bytes("schema-import.json");

        ImportResult yamlResult = importer.importDocument(yaml, ImportOptions.autoDetect());
        ImportResult jsonResult = importer.importDocument(json, ImportOptions.autoDetect());

        assertEquals(ImportStatus.IMPORTED, yamlResult.status());
        assertEquals(ImportStatus.IMPORTED, jsonResult.status());
        assertEquals(Optional.of("3.1.0"), yamlResult.version());
        assertEquals(yamlResult.version(), jsonResult.version());
        assertArrayEquals(yaml, yamlResult.source().content());
        assertArrayEquals(json, jsonResult.source().content());
        assertCustomer(yamlResult.document().orElseThrow());
        assertCustomer(jsonResult.document().orElseThrow());
    }

    @Test
    void testImportDocument_WhenOas30SchemaIsYamlOrJson_ShouldPreserveNullableType() throws IOException {
        byte[] yaml = TestFixtures.bytes("schema-import-3.0.yaml");
        byte[] json = TestFixtures.bytes("schema-import-3.0.json");

        ImportResult yamlResult = importer.importDocument(yaml, ImportOptions.autoDetect());
        ImportResult jsonResult = importer.importDocument(json, ImportOptions.autoDetect());

        assertEquals(ImportStatus.IMPORTED, yamlResult.status());
        assertEquals(ImportStatus.IMPORTED, jsonResult.status());
        assertEquals(Optional.of("3.0.3"), yamlResult.version());
        assertEquals(Optional.of("3.0.0"), jsonResult.version());
        assertArrayEquals(yaml, yamlResult.source().content());
        assertArrayEquals(json, jsonResult.source().content());
        assertNullableCustomer(yamlResult.document().orElseThrow());
        assertNullableCustomer(jsonResult.document().orElseThrow());
    }

    @Test
    void testImportDocument_WhenOas31HasTypeUnion_ShouldPreserveBothTypes() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("schema-types-3.1.yaml"),
                ImportOptions.autoDetect());

        assertEquals(ImportStatus.IMPORTED, result.status());
        assertEquals(Optional.of("3.1.2"), result.version());
        SchemaObject schema = assertInstanceOf(SchemaObject.class,
                result.document().orElseThrow().getComponents().getSchemas().getFirst().getSchema());
        assertEquals(List.of(JsonType.STRING, JsonType.NULL), List.copyOf(schema.getTypes()));
    }

    @Test
    void testImportDocument_WhenVersionIsUnsupported_ShouldNotConvertIt() throws IOException {
        byte[] content = TestFixtures.bytes("unsupported-version.yaml");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(ImportStatus.UNSUPPORTED_VERSION, result.status());
        assertEquals(Optional.of("3.2.0"), result.version());
        assertTrue(result.document().isEmpty());
        assertArrayEquals(content, result.source().content());
    }

    @Test
    void testImportDocument_WhenSwaggerTwoIsProvided_ShouldNotConvertIt() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("swagger-2.yaml"),
                ImportOptions.autoDetect());

        assertEquals(ImportStatus.UNSUPPORTED_VERSION, result.status());
        assertEquals(Optional.of("2.0"), result.version());
        assertTrue(result.document().isEmpty());
    }

    @Test
    void testImportDocument_WhenVersionIsMissing_ShouldKeepSourceWithoutModel() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("missing-version.yaml"),
                ImportOptions.autoDetect());

        assertEquals(ImportStatus.INVALID_OPENAPI, result.status());
        assertTrue(result.version().isEmpty());
        assertTrue(result.document().isEmpty());
        assertFalse(result.diagnostics().isEmpty());
    }

    @Test
    void testImportDocument_WhenOasHasInvalidStructure_ShouldReturnDiagnostic() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("invalid-oas.yaml"),
                ImportOptions.autoDetect());

        assertEquals(Optional.of("3.1.0"), result.version());
        assertFalse(result.diagnostics().isEmpty());
    }

    @Test
    void testImportDocument_WhenParserDropsBooleanSchema_ShouldRejectIncompleteModel() throws IOException {
        byte[] content = TestFixtures.bytes("boolean-schema.yaml");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(ImportStatus.MAPPING_ERROR, result.status());
        assertTrue(result.document().isEmpty());
        assertArrayEquals(content, result.source().content());
    }

    private static void assertCustomer(ApiDocument document) {
        assertEquals("Sample API", document.getInfo().getTitle());
        assertEquals("1.0.0", document.getInfo().getVersion());
        assertEquals(List.of("Customer"), document.getComponents().getSchemas().stream()
                .map(SchemaComponent::getName).toList());
        SchemaObject customer = assertInstanceOf(SchemaObject.class,
                document.getComponents().getSchemas().getFirst().getSchema());
        assertEquals(List.of(JsonType.OBJECT), List.copyOf(customer.getTypes()));
        assertEquals("A customer", customer.getDescription());
        assertEquals(List.of("id", "friend", "external"), customer.getProperties().stream()
                .map(SchemaProperty::getName).toList());
        assertEquals(List.of("id"), List.copyOf(customer.getRequired()));
        SchemaObject friend = assertInstanceOf(SchemaObject.class, customer.getProperties().get(1).getSchema());
        assertEquals("#/components/schemas/Customer", friend.getReference().value());
        SchemaObject external = assertInstanceOf(SchemaObject.class, customer.getProperties().get(2).getSchema());
        assertEquals("https://example.invalid/schemas.yaml#/External", external.getReference().value());
    }

    private static void assertNullableCustomer(ApiDocument document) {
        assertCustomer(document);
        SchemaObject customer = assertInstanceOf(SchemaObject.class,
                document.getComponents().getSchemas().getFirst().getSchema());
        SchemaObject id = assertInstanceOf(SchemaObject.class, customer.getProperties().getFirst().getSchema());
        assertEquals(List.of(JsonType.STRING, JsonType.NULL), List.copyOf(id.getTypes()));
    }
}
