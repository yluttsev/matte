package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.value.BooleanValue;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.NumberValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndexBuilder;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void testImportDocument_WhenOas31UsesExtendedSchemaKeywords_ShouldMapThemInYamlAndJson() throws IOException {
        assertOas31ExtendedSchema("schema-mapping-3.1.yaml");
        assertOas31ExtendedSchema("schema-mapping-3.1.json");
    }

    @Test
    void testImportDocument_WhenOas30UsesExtendedSchemaKeywords_ShouldMapThemInYamlAndJson() throws IOException {
        assertOas30ExtendedSchema("schema-mapping-3.0.yaml");
        assertOas30ExtendedSchema("schema-mapping-3.0.json");
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

    private void assertOas31ExtendedSchema(String fixture) throws IOException {
        byte[] source = TestFixtures.bytes(fixture);
        ImportResult result = importer.importDocument(source, ImportOptions.autoDetect());

        assertEquals(ImportStatus.IMPORTED, result.status(), result.diagnostics().toString());
        assertArrayEquals(source, result.source().content());
        SchemaObject rich = richSchema(result.document().orElseThrow());
        assertFalse(assertInstanceOf(BooleanSchema.class, property(rich, "noItems").getItems()).getValue());
        assertTrue(assertInstanceOf(BooleanSchema.class, property(rich, "open").getAdditionalProperties())
                .getValue());
        assertEquals(new NullValue(), property(rich, "nullExample").getExample());
        SchemaObject list = property(rich, "list");
        assertEquals(BigInteger.ONE, list.getMinItems());
        assertEquals(BigInteger.valueOf(5), list.getMaxItems());
        assertEquals(Boolean.TRUE, list.getUniqueItems());
        assertEquals("#/components/schemas/Base", schemaObject(list.getItems()).getReference().value());

        SchemaObject dictionary = property(rich, "dictionary");
        assertEquals(BigInteger.ONE, dictionary.getMinProperties());
        assertEquals(BigInteger.valueOf(4), dictionary.getMaxProperties());
        assertEquals(List.of(JsonType.STRING), List.copyOf(schemaObject(dictionary.getAdditionalProperties())
                .getTypes()));
        assertFalse(assertInstanceOf(BooleanSchema.class, property(rich, "closed").getAdditionalProperties())
                .getValue());

        SchemaObject text = property(rich, "text");
        assertEquals("password", text.getFormat());
        assertEquals(BigInteger.TWO, text.getMinLength());
        assertEquals(BigInteger.valueOf(12), text.getMaxLength());
        assertEquals("^[a-z]+$", text.getPattern());
        assertEquals(new NullValue(), text.getDefaultValue());
        assertEquals(new ObjectValue(Map.of(
                "marker", new StringValue("#/components/schemas/Base"),
                "const", new NullValue())), text.getExample());
        assertEquals(List.of(new NullValue(), new ObjectValue(Map.of(
                "enabled", new BooleanValue(true),
                "count", new NumberValue(BigDecimal.valueOf(3)),
                "ratio", new NumberValue(new BigDecimal("1.25"))))), text.getExamples());
        assertEquals(List.of(new NullValue(), new StringValue("active")), text.getEnumValues());
        assertEquals(new NullValue(), text.getConstValue());
        assertEquals(Boolean.FALSE, text.getDeprecated());
        assertEquals(Boolean.FALSE, text.getReadOnly());
        assertEquals(Boolean.TRUE, text.getWriteOnly());

        SchemaObject amount = property(rich, "amount");
        assertEquals(new BigDecimal("1.25"), amount.getMinimum());
        assertEquals(new BigDecimal("9.75"), amount.getMaximum());
        assertEquals(new BigDecimal("1.5"), amount.getExclusiveMinimum());
        assertEquals(new BigDecimal("9"), amount.getExclusiveMaximum());
        assertEquals(new BigDecimal("0.25"), amount.getMultipleOf());
        assertCompositionAndReferences(result.document().orElseThrow(), rich,
                "#/components/schemas/Base", JsonType.NULL);
        assertAbsentValues(result.document().orElseThrow());
    }

    private void assertOas30ExtendedSchema(String fixture) throws IOException {
        byte[] source = TestFixtures.bytes(fixture);
        ImportResult result = importer.importDocument(source, ImportOptions.autoDetect());

        assertEquals(ImportStatus.IMPORTED, result.status(), result.diagnostics().toString());
        assertArrayEquals(source, result.source().content());
        SchemaObject rich = richSchema(result.document().orElseThrow());
        SchemaObject list = property(rich, "list");
        assertEquals(BigInteger.ONE, list.getMinItems());
        assertEquals(BigInteger.valueOf(5), list.getMaxItems());
        assertEquals(Boolean.TRUE, list.getUniqueItems());
        assertEquals("#/components/schemas/Base", schemaObject(list.getItems()).getReference().value());

        SchemaObject dictionary = property(rich, "dictionary");
        assertEquals(BigInteger.ONE, dictionary.getMinProperties());
        assertEquals(BigInteger.valueOf(4), dictionary.getMaxProperties());
        assertEquals(List.of(JsonType.STRING), List.copyOf(schemaObject(dictionary.getAdditionalProperties())
                .getTypes()));
        assertFalse(assertInstanceOf(BooleanSchema.class, property(rich, "closed").getAdditionalProperties())
                .getValue());

        SchemaObject text = property(rich, "text");
        assertEquals(new NullValue(), property(rich, "nullExample").getExample());
        assertEquals(List.of(JsonType.STRING, JsonType.NULL), List.copyOf(text.getTypes()));
        assertEquals(new NullValue(), text.getDefaultValue());
        assertEquals(new ObjectValue(Map.of("marker", new StringValue("#/components/schemas/Base"))),
                text.getExample());
        assertTrue(text.getExamples().isEmpty());
        assertNull(text.getConstValue());
        assertEquals(List.of(new NullValue(), new StringValue("active")), text.getEnumValues());
        assertEquals(Boolean.FALSE, text.getDeprecated());
        assertEquals(Boolean.FALSE, text.getReadOnly());
        assertEquals(Boolean.TRUE, text.getWriteOnly());

        SchemaObject amount = property(rich, "amount");
        assertEquals(new BigDecimal("1.25"), amount.getMinimum());
        assertEquals(new BigDecimal("9.75"), amount.getMaximum());
        assertEquals(new BigDecimal("1.25"), amount.getExclusiveMinimum());
        assertEquals(new BigDecimal("9.75"), amount.getExclusiveMaximum());
        assertEquals(new BigDecimal("0.25"), amount.getMultipleOf());
        assertCompositionAndReferences(result.document().orElseThrow(), rich, "Base", JsonType.STRING);
        assertAbsentValues(result.document().orElseThrow());
    }

    private void assertCompositionAndReferences(ApiDocument document, SchemaObject rich, String mappingValue,
            JsonType notType) {
        SchemaObject choice = property(rich, "choice");
        assertEquals(1, choice.getAllOf().size());
        assertEquals(1, choice.getOneOf().size());
        assertEquals(1, choice.getAnyOf().size());
        assertEquals(List.of(notType), List.copyOf(schemaObject(choice.getNot()).getTypes()));
        Discriminator discriminator = choice.getDiscriminator();
        assertEquals("kind", discriminator.getPropertyName());
        assertEquals(mappingValue, discriminator.getMapping().get("base").value());

        ReferenceIndex references = new ReferenceIndexBuilder().build(DocumentIndex.build(document));
        assertTrue(references.unresolvedUsages().isEmpty(), references.unresolvedUsages().toString());
        DocumentPath richPath = DocumentPath.schemaComponent("Rich");
        assertTrue(references.findBySource(richPath.child("properties").child("list").child("items")
                .child("$ref")).isPresent());
        assertTrue(references.findBySource(richPath.child("properties").child("choice").child("allOf")
                .child("0").child("$ref")).isPresent());
        assertTrue(references.findBySource(richPath.child("properties").child("choice").child("discriminator")
                .child("mapping").child("base")).isPresent());
    }

    private void assertAbsentValues(ApiDocument document) {
        SchemaObject base = assertInstanceOf(SchemaObject.class,
                document.getComponents().getSchemas().getFirst().getSchema());
        assertNull(base.getDefaultValue());
        assertNull(base.getExample());
        assertNull(base.getConstValue());
    }

    private static SchemaObject richSchema(ApiDocument document) {
        return assertInstanceOf(SchemaObject.class, document.getComponents().getSchemas().getLast().getSchema());
    }

    private static SchemaObject property(SchemaObject schema, String name) {
        return schema.getProperties().stream()
                .filter(property -> property.getName().equals(name))
                .map(SchemaProperty::getSchema)
                .map(CoreImportTest::schemaObject)
                .findFirst()
                .orElseThrow();
    }

    private static SchemaObject schemaObject(Schema schema) {
        return assertInstanceOf(SchemaObject.class, schema);
    }
}
