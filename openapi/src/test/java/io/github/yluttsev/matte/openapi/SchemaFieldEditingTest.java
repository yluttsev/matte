package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaFieldEditingTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testUpdateSchema_WhenYamlFieldsAreSetAndCleared_ShouldPreserveOtherSourceFields() throws IOException {
        OpenApiEditResult result = updateSchemaFromFixture("schema-field-editing-3.1.yaml");

        assertInstanceOf(EditSucceeded.class, result.edit());
        assertTrue(text(result).contains("title: \"Updated title\""));
        assertTrue(text(result).contains("type: [\"null\",\"string\"]"), text(result));
        assertTrue(text(result).contains("format: email"));
        assertTrue(text(result).contains("deprecated: true"));
        assertTrue(text(result).contains("readOnly: false"));
        assertTrue(text(result).contains("writeOnly: true"));
        assertTrue(text(result).contains("description: \"Updated description\""));
        assertTrue(text(result).contains("example: null"));
        assertTrue(text(result).contains("const: null"));
        assertTrue(text(result).contains("enum: [null,\"active\"]"));
        assertTrue(text(result).contains("x-note:\n        $ref: '#/components/schemas/Customer'"));
        assertTrue(text(result).contains("properties:\n        name:\n          type: string\n"
                + "          x-note:\n            $ref: '#/components/schemas/Customer'"));
        assertFalse(text(result).contains("default:"));
        assertFalse(text(result).contains("minLength:"));
        SchemaObject updated = customer(result.state());
        assertEquals(Set.of(JsonType.STRING, JsonType.NULL), updated.getTypes());
        assertEquals("email", updated.getFormat());
        assertEquals(Boolean.TRUE, updated.getDeprecated());
        assertEquals(Boolean.FALSE, updated.getReadOnly());
        assertEquals(Boolean.TRUE, updated.getWriteOnly());
        assertNull(updated.getDefaultValue());
        assertEquals(new NullValue(), updated.getExample());
        assertEquals(new NullValue(), updated.getConstValue());
        assertEquals(List.of(new NullValue(), new StringValue("active")), updated.getEnumValues());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    void testUpdateSchema_WhenJsonFieldsAreSetAndCleared_ShouldPreserveOtherSourceFields() throws IOException {
        OpenApiEditResult result = updateSchemaFromFixture("schema-field-editing-3.1.json");

        assertInstanceOf(EditSucceeded.class, result.edit());
        assertTrue(text(result).contains("\"title\": \"Updated title\""));
        assertTrue(text(result).contains("\"type\": [\"null\",\"string\"]"), text(result));
        assertTrue(text(result).contains("\"format\": \"email\""));
        assertTrue(text(result).contains("\"deprecated\": true"));
        assertTrue(text(result).contains("\"readOnly\": false"));
        assertTrue(text(result).contains("\"writeOnly\": true"));
        assertTrue(text(result).contains("\"example\": null"));
        assertTrue(text(result).contains("\"const\": null"));
        assertTrue(text(result).contains("\"enum\": [null,\"active\"]"));
        assertTrue(text(result).contains("\"description\": \"Updated description\""));
        assertTrue(text(result).contains("\"x-note\": {\"$ref\": \"#/components/schemas/Customer\"}"));
        assertTrue(text(result).contains("\"name\": {\n            \"type\": \"string\",\n"
                + "            \"x-note\": {\"$ref\": \"#/components/schemas/Customer\"}\n          }"));
        assertFalse(text(result).contains("\"default\""));
        assertFalse(text(result).contains("\"minLength\""));
        SchemaObject updated = customer(result.state());
        assertEquals(Set.of(JsonType.STRING, JsonType.NULL), updated.getTypes());
        assertEquals("email", updated.getFormat());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(Boolean.TRUE, updated.getDeprecated());
        assertEquals(Boolean.FALSE, updated.getReadOnly());
        assertEquals(Boolean.TRUE, updated.getWriteOnly());
        assertNull(updated.getDefaultValue());
        assertEquals(new NullValue(), updated.getExample());
        assertEquals(new NullValue(), updated.getConstValue());
    }

    @Test
    void testUpdateSchema_WhenNoFieldsChange_ShouldPreserveExactSourceBytes() throws IOException {
        ImportResult imported = importFixture("schema-field-editing-noop.yaml");
        SchemaObject schema = customer(imported);
        SchemaObject unchanged = SchemaObject.builder(schema).build();

        OpenApiEditResult result = editor.updateSchema(imported, new UpdateSchemaObject(unchanged));

        assertArrayEquals(imported.source().content(), result.source().orElseThrow().content());
        assertSame(imported.source(), result.source().orElseThrow());
    }

    @Test
    void testUpdateSchema_WhenAddedFieldsAreChangedAndCleared_ShouldKeepSourceAndCoreInSync() throws IOException {
        ImportResult imported = importFixture("additions-empty.yaml");
        SchemaObject schema = customer(importFixture("schema-field-editing-add-schema.yaml"));
        OpenApiEditResult added = editor.addComponent(imported, new AddSchemaComponent(
                new SchemaComponent(ElementId.random(), "Customer", schema)));
        SchemaObject addedSchema = customer(added.state());
        SchemaObject replacement = SchemaObject.builder(addedSchema)
                .title("After")
                .defaultValue(null)
                .minLength(null)
                .build();

        OpenApiEditResult updated = editor.updateSchema(added.state(), new UpdateSchemaObject(replacement));

        assertInstanceOf(EditSucceeded.class, updated.edit());
        assertTrue(text(updated).contains("title: \"After\""));
        assertFalse(text(updated).contains("default:"));
        assertFalse(text(updated).contains("minLength:"));
        assertEquals("After", customer(updated.state()).getTitle());
        assertNull(customer(updated.state()).getDefaultValue());
        assertNull(customer(updated.state()).getMinLength());
    }

    @Test
    void testUpdateSchema_WhenOas30ExclusiveBoundsChange_ShouldWriteLegacyKeywords() throws IOException {
        ImportResult imported = importFixture("schema-field-editing-3.0-bounds.json");
        SchemaObject schema = customer(imported);
        SchemaObject replacement = SchemaObject.builder(schema)
                .types(Set.of(JsonType.NUMBER, JsonType.NULL))
                .minimum(new BigDecimal("1.5"))
                .exclusiveMinimum(new BigDecimal("1.5"))
                .maximum(new BigDecimal("9"))
                .exclusiveMaximum(new BigDecimal("9"))
                .defaultValue(null)
                .example(new NullValue())
                .build();

        OpenApiEditResult result = editor.updateSchema(imported, new UpdateSchemaObject(replacement));

        assertInstanceOf(EditSucceeded.class, result.edit());
        assertTrue(text(result).contains("\"minimum\": 1.5"), text(result));
        assertTrue(text(result).contains("\"exclusiveMinimum\": true"), text(result));
        assertTrue(text(result).contains("\"maximum\": 9"), text(result));
        assertTrue(text(result).contains("\"exclusiveMaximum\": true"), text(result));
        assertTrue(text(result).contains("\"nullable\": true"));
        assertTrue(text(result).contains("\"example\": null"));
        assertFalse(text(result).contains("\"default\""));
        assertEquals(Set.of(JsonType.NUMBER, JsonType.NULL), customer(result.state()).getTypes());
    }

    @Test
    void testUpdateSchema_WhenOas30FieldIsUnavailable_ShouldRejectWithoutPatchingSource() throws IOException {
        ImportResult imported = importFixture("schema-field-editing-3.0-const.yaml");
        SchemaObject schema = customer(imported);
        SchemaObject replacement = SchemaObject.builder(schema).constValue(new NullValue()).build();
        byte[] original = imported.source().content();

        assertThrows(IllegalArgumentException.class,
                () -> editor.updateSchema(imported, new UpdateSchemaObject(replacement)));

        assertArrayEquals(original, imported.source().content());
    }

    @Test
    void testUpdateSchema_WhenRemovingFieldWithInlineComment_ShouldRejectWithoutPatchingSource() throws IOException {
        ImportResult imported = importFixture("schema-field-editing-inline-comment.yaml");
        SchemaObject schema = customer(imported);
        SchemaObject replacement = SchemaObject.builder(schema).minLength(null).build();
        byte[] original = imported.source().content();

        assertThrows(IllegalArgumentException.class,
                () -> editor.updateSchema(imported, new UpdateSchemaObject(replacement)));

        assertArrayEquals(original, imported.source().content());
    }

    private OpenApiEditResult updateSchemaFromFixture(String fixtureName) throws IOException {
        ImportResult imported = importFixture(fixtureName);
        SchemaObject schema = customer(imported);
        SchemaObject replacement = SchemaObject.builder(schema)
                .types(Set.of(JsonType.STRING, JsonType.NULL))
                .title("Updated title")
                .description("Updated description")
                .format("email")
                .deprecated(true)
                .readOnly(false)
                .writeOnly(true)
                .defaultValue(null)
                .minLength(null)
                .example(new NullValue())
                .constValue(new NullValue())
                .enumValues(List.of(new NullValue(), new StringValue("active")))
                .build();
        return editor.updateSchema(imported, new UpdateSchemaObject(replacement));
    }

    private ImportResult importFixture(String fixtureName) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(fixtureName), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaObject customer(ImportResult imported) {
        return customer(imported.document().orElseThrow());
    }

    private SchemaObject customer(ApiDocument document) {
        return (SchemaObject) document.getComponents().getSchemas().getFirst().getSchema();
    }

    private String text(OpenApiEditResult result) {
        return new String(result.source().orElseThrow().content(), StandardCharsets.UTF_8);
    }
}
