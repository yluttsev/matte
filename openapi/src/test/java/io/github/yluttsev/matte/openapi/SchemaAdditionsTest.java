package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaAdditionsTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testAddComponent_WhenYamlHasNoComponents_ShouldCreateMissingSections() throws IOException {
        ImportResult imported = importFixture("additions-empty.yaml");

        OpenApiEditResult result = editor.addComponent(imported, component("Customer"));

        assertInstanceOf(EditSucceeded.class, result.edit());
        String expected = new String(TestFixtures.bytes("additions-empty.yaml"), StandardCharsets.UTF_8)
                + "components:\n  schemas:\n    Customer:\n      type: object\n";
        assertEquals(expected, text(result));
        assertComponent(result, "Customer");
    }

    @Test
    void testAddComponent_WhenJsonHasNoComponents_ShouldCreateMissingSections() throws IOException {
        ImportResult imported = importFixture("additions-empty.json");

        OpenApiEditResult result = editor.addComponent(imported, component("Customer"));

        String original = new String(TestFixtures.bytes("additions-empty.json"), StandardCharsets.UTF_8);
        int close = original.lastIndexOf('}');
        String expected = original.substring(0, close)
                + ",\"components\":{\"schemas\":{\"Customer\":{\"type\":\"object\"}}}"
                + original.substring(close);
        assertEquals(expected, text(result));
        assertComponent(result, "Customer");
    }

    @Test
    void testAddComponent_WhenOas30YamlHasSchemas_ShouldPreserveExistingFields() throws IOException {
        ImportResult imported = importFixture("schema-import-3.0.yaml");
        byte[] original = imported.source().content();

        OpenApiEditResult result = editor.addComponent(imported, component("Address"));

        assertEquals(ImportStatus.IMPORTED, result.state().status());
        assertTrue(text(result).contains("    Address:\n      type: object\n"));
        assertTrue(text(result).contains("      nullable: true"));
        assertArrayEquals(original, imported.source().content());
        assertEquals("Address", result.state().document().orElseThrow().getComponents().getSchemas()
                .getLast().getName());
        assertComponent(result, "Address");
    }

    @Test
    void testAddComponent_WhenComponentsIsFlowMapping_ShouldInsertIntoIt() throws IOException {
        ImportResult imported = importFixture("additions-sections.yaml");

        OpenApiEditResult result = editor.addComponent(imported, component("Customer"));

        assertTrue(text(result).contains("components: {\"schemas\":{\"Customer\":{\"type\":\"object\"}}}"));
        assertComponent(result, "Customer");
    }

    @Test
    void testAddComponent_WhenNameNeedsYamlQuoting_ShouldKeepItsMeaning() throws IOException {
        ImportResult imported = importFixture("additions-empty.yaml");

        OpenApiEditResult result = editor.addComponent(imported, component("null"));

        assertTrue(text(result).contains("    \"null\":\n      type: object"));
        assertComponent(result, "null");
    }

    @Test
    void testAddComponent_WhenYamlHasSchemas_ShouldAppendWithoutChangingOtherSections() throws IOException {
        ImportResult imported = importFixture("schema-import.yaml");

        OpenApiEditResult result = editor.addComponent(imported, component("Address"));

        String original = new String(TestFixtures.bytes("schema-import.yaml"), StandardCharsets.UTF_8);
        String expected = original.replace("x-internal:", "    Address:\n      type: object\nx-internal:");
        assertEquals(expected, text(result));
        assertEquals(2, importer.importDocument(result.source().orElseThrow().content(), ImportOptions.autoDetect())
                .document().orElseThrow().getComponents().getSchemas().size());
    }

    @Test
    void testAddComponent_WhenSchemaHasNestedProperties_ShouldPreserveTheirMeaning() throws IOException {
        ImportResult imported = importFixture("additions-empty.yaml");
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.OBJECT))
                .required(Set.of("email"))
                .properties(List.of(new SchemaProperty(ElementId.random(), "email", stringSchema())))
                .build();

        OpenApiEditResult result = editor.addComponent(imported,
                new AddSchemaComponent(new SchemaComponent(ElementId.random(), "Customer", schema)));

        assertTrue(text(result).contains("        email:\n          type: string"));
        SchemaObject added = customer(importer.importDocument(result.source().orElseThrow().content(),
                ImportOptions.autoDetect()));
        assertEquals(Set.of("email"), added.getRequired());
        assertEquals("email", added.getProperties().getFirst().getName());
    }

    @Test
    void testAddComponent_WhenOas30SchemaIsNullable_ShouldWriteNullableKeyword() throws IOException {
        ImportResult imported = importFixture("additions-empty.json");
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.STRING, JsonType.NULL)).build();

        OpenApiEditResult result = editor.addComponent(imported,
                new AddSchemaComponent(new SchemaComponent(ElementId.random(), "MaybeName", schema)));

        assertTrue(text(result).contains("\"type\":\"string\",\"nullable\":true"));
    }

    @Test
    void testAddComponent_WhenOas31SchemaAllowsNull_ShouldWriteTypeUnion() throws IOException {
        ImportResult imported = importFixture("additions-empty.yaml");
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.STRING, JsonType.NULL)).build();

        OpenApiEditResult result = editor.addComponent(imported,
                new AddSchemaComponent(new SchemaComponent(ElementId.random(), "MaybeName", schema)));

        assertTrue(text(result).contains("type: ["));
        assertTrue(text(result).contains("\"null\""));
    }

    @Test
    void testAddProperty_WhenYamlHasNoProperties_ShouldCreateSection() throws IOException {
        ImportResult imported = importFixture("additions-no-properties.yaml");
        SchemaObject schema = customer(imported);

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(schema.getId(),
                new SchemaProperty(ElementId.random(), "email", stringSchema())));

        String expected = new String(TestFixtures.bytes("additions-no-properties.yaml"), StandardCharsets.UTF_8)
                + "      properties:\n        email:\n          type: string\n";
        assertEquals(expected, text(result));
        assertProperty(result, "email");
    }

    @Test
    void testAddProperty_WhenYamlHasExistingProperties_ShouldPreserveRemainingText() throws IOException {
        ImportResult imported = importFixture("schema-import.yaml");
        SchemaObject schema = customer(imported);

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(schema.getId(),
                new SchemaProperty(ElementId.random(), "email", stringSchema())));

        String original = new String(TestFixtures.bytes("schema-import.yaml"), StandardCharsets.UTF_8);
        String expected = original.replace("x-internal:", "        email:\n          type: string\nx-internal:");
        assertEquals(expected, text(result));
        assertProperty(result, "email");
    }

    @Test
    void testAddProperty_WhenOas30YamlHasExistingProperties_ShouldKeepSourceAndCoreConsistent() throws IOException {
        ImportResult imported = importFixture("schema-import-3.0.yaml");
        byte[] original = imported.source().content();
        SchemaObject schema = customer(imported);
        ElementId propertyId = ElementId.random();

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(schema.getId(),
                new SchemaProperty(propertyId, "email", stringSchema())));

        assertTrue(text(result).contains("        email:\n          type: string\n"));
        assertArrayEquals(original, imported.source().content());
        SchemaObject edited = customer(result.state());
        assertEquals(propertyId, edited.getProperties().getLast().getId());
        assertEquals("email", edited.getProperties().getLast().getName());
        assertProperty(result, "email");
    }

    @Test
    void testAddProperty_WhenSchemaIsNested_ShouldInsertAtItsOwnPath() throws IOException {
        ImportResult imported = importFixture("additions-nested.yaml");
        SchemaObject address = assertInstanceOf(SchemaObject.class,
                customer(imported).getProperties().getFirst().getSchema());

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(address.getId(),
                new SchemaProperty(ElementId.random(), "city", stringSchema())));

        String expected = new String(TestFixtures.bytes("additions-nested.yaml"), StandardCharsets.UTF_8)
                + "          properties:\n            city:\n              type: string\n";
        assertEquals(expected, text(result));
        assertInstanceOf(EditSucceeded.class, result.edit());
    }

    @Test
    void testAddProperty_WhenJsonHasExistingProperties_ShouldPreserveRemainingText() throws IOException {
        ImportResult imported = importFixture("schema-import.json");
        SchemaObject schema = customer(imported);

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(schema.getId(),
                new SchemaProperty(ElementId.random(), "email", stringSchema())));

        assertTrue(text(result).contains("\"email\":{\"type\":\"string\"}"));
        assertProperty(result, "email");
        assertArrayEquals(TestFixtures.bytes("schema-import.json"), imported.source().content());
    }

    @Test
    void testAddProperty_WhenOas30JsonHasExistingProperties_ShouldPreserveNullableAndReferences() throws IOException {
        ImportResult imported = importFixture("schema-import-3.0.json");
        byte[] original = imported.source().content();
        SchemaObject schema = customer(imported);

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(schema.getId(),
                new SchemaProperty(ElementId.random(), "email", stringSchema())));

        assertTrue(text(result).contains("\"email\":{\"type\":\"string\"}"));
        assertTrue(text(result).contains("\"nullable\":true"));
        assertTrue(text(result).contains("#/components/schemas/Customer"));
        assertArrayEquals(original, imported.source().content());
        assertProperty(result, "email");
    }

    @Test
    void testAddProperty_WhenSourceHasBomAndCrlf_ShouldKeepBoth() throws IOException {
        String bom = Character.toString((char) 0xFEFF);
        String original = bom + new String(TestFixtures.bytes("additions-no-properties.yaml"), StandardCharsets.UTF_8)
                .replace("\n", "\r\n");
        ImportResult imported = importer.importDocument(original.getBytes(StandardCharsets.UTF_8),
                ImportOptions.autoDetect());

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(customer(imported).getId(),
                new SchemaProperty(ElementId.random(), "email", stringSchema())));

        assertEquals(original + "      properties:\r\n        email:\r\n          type: string\r\n", text(result));
    }

    @Test
    void testAddProperty_WhenJsonPropertiesIsEmpty_ShouldInsertIntoFlowObject() throws IOException {
        ImportResult imported = importFixture("additions-empty-properties.json");

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(customer(imported).getId(),
                new SchemaProperty(ElementId.random(), "email", stringSchema())));

        assertTrue(text(result).contains("\"properties\":{\"email\":{\"type\":\"string\"}}"));
    }

    @Test
    void testAddProperty_WhenJsonIsFormatted_ShouldKeepUnchangedWhitespace() throws IOException {
        ImportResult imported = importFixture("additions-formatted.json");

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(customer(imported).getId(),
                new SchemaProperty(ElementId.random(), "email", stringSchema())));

        String original = new String(TestFixtures.bytes("additions-formatted.json"), StandardCharsets.UTF_8);
        String expected = original.replace("\"id\": {\"type\": \"string\"}",
                "\"id\": {\"type\": \"string\"},\n          \"email\": {\"type\":\"string\"}");
        assertEquals(expected, text(result));
    }

    @Test
    void testAddComponent_WhenJsonSchemasIsEmpty_ShouldInsertIntoIt() throws IOException {
        ImportResult imported = importFixture("additions-empty-sections.json");

        OpenApiEditResult result = editor.addComponent(imported, component("Customer"));

        assertTrue(text(result).contains("\"schemas\":{\"Customer\":{\"type\":\"object\"}}"));
        assertComponent(result, "Customer");
    }

    @Test
    void testAddProperty_WhenNameConflicts_ShouldNotPatchSource() throws IOException {
        ImportResult imported = importFixture("schema-import.yaml");
        SchemaObject schema = customer(imported);

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(schema.getId(),
                new SchemaProperty(ElementId.random(), "id", stringSchema())));

        assertInstanceOf(EditRejected.class, result.edit());
        assertTrue(result.source().isEmpty());
        assertArrayEquals(imported.source().content(), result.state().source().content());
    }

    @Test
    void testAddComponent_WhenNameConflicts_ShouldNotPatchSource() throws IOException {
        ImportResult imported = importFixture("schema-import.yaml");

        OpenApiEditResult result = editor.addComponent(imported, component("Customer"));

        assertInstanceOf(EditRejected.class, result.edit());
        assertTrue(result.source().isEmpty());
        assertArrayEquals(imported.source().content(), result.state().source().content());
    }

    @Test
    void testAddComponent_WhenElementIdConflicts_ShouldNotPatchSource() throws IOException {
        ImportResult imported = importFixture("schema-import.yaml");
        SchemaComponent existing = imported.document().orElseThrow().getComponents().getSchemas().getFirst();
        byte[] original = imported.source().content();

        OpenApiEditResult result = editor.addComponent(imported, new AddSchemaComponent(
                new SchemaComponent(existing.getId(), "Address", stringSchema())));

        assertInstanceOf(EditRejected.class, result.edit());
        assertTrue(result.source().isEmpty());
        assertArrayEquals(original, result.state().source().content());
        assertEquals(1, result.state().document().orElseThrow().getComponents().getSchemas().size());
    }

    @Test
    void testAddProperty_WhenElementIdConflicts_ShouldNotPatchSource() throws IOException {
        ImportResult imported = importFixture("additions-no-properties.yaml");
        SchemaObject schema = customer(imported);

        OpenApiEditResult result = editor.addProperty(imported, new AddSchemaProperty(schema.getId(),
                new SchemaProperty(schema.getId(), "email", stringSchema())));

        assertInstanceOf(EditRejected.class, result.edit());
        assertTrue(result.source().isEmpty());
    }

    @Test
    void testAddProperty_WhenSourceAndModelDisagree_ShouldRejectPatch() throws IOException {
        ImportResult imported = importFixture("additions-no-properties.yaml");
        ImportResult mismatched = new ImportResult(new SourceDocument(TestFixtures.bytes("additions-empty.yaml")),
                imported.format(), imported.status(), imported.diagnostics(), imported.version(), imported.document());

        assertThrows(IllegalArgumentException.class, () -> editor.addProperty(mismatched,
                new AddSchemaProperty(customer(imported).getId(),
                        new SchemaProperty(ElementId.random(), "email", stringSchema()))));
    }

    @Test
    void testAddComponent_WhenSchemaContainsUnsupportedFields_ShouldFailClosed() throws IOException {
        ImportResult imported = importFixture("additions-empty.yaml");
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.STRING)).pattern("[a-z]+ ").build();

        assertThrows(IllegalArgumentException.class, () -> editor.addComponent(imported,
                new AddSchemaComponent(new SchemaComponent(ElementId.random(), "Name", schema))));
    }

    @Test
    void testAddComponent_WhenSchemaIsBoolean_ShouldRejectIncompleteParserRoundTrip() throws IOException {
        ImportResult imported = importFixture("additions-empty.yaml");

        assertThrows(IllegalArgumentException.class, () -> editor.addComponent(imported,
                new AddSchemaComponent(new SchemaComponent(ElementId.random(), "Always",
                        new BooleanSchema(ElementId.random(), true)))));
    }

    private ImportResult importFixture(String name) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(name), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status());
        return imported;
    }

    private AddSchemaComponent component(String name) {
        return new AddSchemaComponent(new SchemaComponent(ElementId.random(), name,
                SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.OBJECT)).build()));
    }

    private SchemaObject stringSchema() {
        return SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.STRING)).build();
    }

    private SchemaObject customer(ImportResult imported) {
        return assertInstanceOf(SchemaObject.class,
                imported.document().orElseThrow().getComponents().getSchemas().getFirst().getSchema());
    }

    private String text(OpenApiEditResult result) {
        return new String(result.source().orElseThrow().content(), StandardCharsets.UTF_8);
    }

    private void assertComponent(OpenApiEditResult result, String name) {
        ImportResult checked = importer.importDocument(result.source().orElseThrow().content(),
                ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, checked.status());
        assertTrue(checked.document().orElseThrow().getComponents().getSchemas().stream()
                .anyMatch(component -> component.getName().equals(name)));
    }

    private void assertProperty(OpenApiEditResult result, String name) {
        ImportResult checked = importer.importDocument(result.source().orElseThrow().content(),
                ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, checked.status());
        SchemaObject schema = customer(checked);
        assertEquals(name, schema.getProperties().getLast().getName());
        assertEquals(List.of(JsonType.STRING), List.copyOf(
                assertInstanceOf(SchemaObject.class, schema.getProperties().getLast().getSchema()).getTypes()));
    }
}
