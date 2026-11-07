package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.value.ArrayValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlAliasSafetyTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testAddProperty_WhenSchemaNodeIsAnchoredAndAliased_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("yaml-alias-schema.yaml");
        SchemaObject schema = schema(imported, "Shared");

        assertAliasRefusal(
                imported,
                () -> editor.addProperty(imported, new AddSchemaProperty(schema.getId(), property("email")))
        );
        assertEquals(1, schema(imported, "Shared").getProperties().size());
    }

    @Test
    void testDeleteProperty_WhenSchemaNodeIsAnchoredAndAliased_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("yaml-alias-schema.yaml");
        SchemaObject schema = schema(imported, "Shared");
        SchemaProperty property = schema.getProperties().getFirst();

        assertAliasRefusal(
                imported,
                () -> editor.deleteProperty(
                        imported,
                        new DeleteSchemaProperty(schema.getId(), property.getId())
                )
        );
        assertEquals(1, schema(imported, "Shared").getProperties().size());
    }

    @Test
    void testUpdateSchema_WhenSchemaNodeIsAnchoredAndAliased_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("yaml-alias-schema.yaml");
        SchemaObject schema = schema(imported, "Shared");
        SchemaObject updated = SchemaObject.builder(schema).title("Changed").build();

        assertAliasRefusal(
                imported,
                () -> editor.updateSchema(imported, new UpdateSchemaObject(updated))
        );
        assertEquals("Shared schema", schema(imported, "Shared").getTitle());
    }

    @Test
    void testAddProperty_WhenPropertiesMappingIsAliased_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("yaml-alias-mapping.yaml");
        SchemaObject schema = schema(imported, "First");

        assertAliasRefusal(
                imported,
                () -> editor.addProperty(imported, new AddSchemaProperty(schema.getId(), property("email")))
        );
        assertEquals(1, schema(imported, "First").getProperties().size());
    }

    @Test
    void testDeleteProperty_WhenPropertiesMappingIsAliased_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("yaml-alias-mapping.yaml");
        SchemaObject schema = schema(imported, "First");
        SchemaProperty property = schema.getProperties().getFirst();

        assertAliasRefusal(
                imported,
                () -> editor.deleteProperty(
                        imported,
                        new DeleteSchemaProperty(schema.getId(), property.getId())
                )
        );
        assertEquals(1, schema(imported, "First").getProperties().size());
    }

    @Test
    void testUpdateSchema_WhenParentMappingIsAliased_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("yaml-alias-mapping.yaml");
        SchemaObject parent = schema(imported, "First");
        SchemaObject nested = (SchemaObject) parent.getProperties().getFirst().getSchema();
        SchemaObject updated = SchemaObject.builder(nested).title("Changed").build();

        assertAliasRefusal(
                imported,
                () -> editor.updateSchema(imported, new UpdateSchemaObject(updated))
        );
        assertEquals("Nested schema", nested.getTitle());
    }

    @Test
    void testUpdateSchema_WhenNestedSchemaSequenceIsAliased_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("yaml-alias-sequence.yaml");
        SchemaObject composite = schema(imported, "Composite");
        SchemaObject nested = (SchemaObject) composite.getAllOf().getFirst();
        SchemaObject updated = SchemaObject.builder(nested).title("Changed").build();

        assertAliasRefusal(
                imported,
                () -> editor.updateSchema(imported, new UpdateSchemaObject(updated))
        );
        assertEquals("Nested schema", nested.getTitle());
    }

    @Test
    void testUpdateSchema_WhenChangedValueContainsAliasedSequence_ShouldRejectWithoutChangingSource()
            throws IOException {
        ImportResult imported = importFixture("yaml-alias-value-subtree.yaml");
        SchemaObject schema = schema(imported, "Values");
        ObjectValue updatedDefault = new ObjectValue(Map.of(
                "nested",
                new ObjectValue(Map.of(
                        "values",
                        new ArrayValue(List.of(new StringValue("changed")))
                ))
        ));
        SchemaObject updated = SchemaObject.builder(schema).defaultValue(updatedDefault).build();

        assertAliasRefusal(
                imported,
                () -> editor.updateSchema(imported, new UpdateSchemaObject(updated))
        );
        assertEquals(schema.getDefaultValue(), schema(imported, "Values").getDefaultValue());
    }

    @Test
    void testUpdateSchema_WhenAliasIsOutsideChangedSchema_ShouldPreserveModelConsistency() throws IOException {
        ImportResult imported = importFixture("yaml-alias-schema.yaml");
        SchemaObject independent = schema(imported, "Independent");
        SchemaObject updated = SchemaObject.builder(independent).title("Updated independent").build();

        OpenApiEditResult result = editor.updateSchema(imported, new UpdateSchemaObject(updated));
        ImportResult checked = importer.importDocument(
                result.source().orElseThrow().content(),
                ImportOptions.autoDetect()
        );

        assertInstanceOf(EditSucceeded.class, result.edit());
        assertEquals("Updated independent", schema(result.state(), "Independent").getTitle());
        assertEquals("Updated independent", schema(checked, "Independent").getTitle());
        assertEquals("Shared schema", schema(checked, "Alias").getTitle());
    }

    private void assertAliasRefusal(ImportResult imported, Runnable edit) {
        byte[] originalSource = imported.source().content();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, edit::run);

        assertTrue(error.getMessage().contains("YAML aliases"), error.getMessage());
        assertArrayEquals(originalSource, imported.source().content());
    }

    private ImportResult importFixture(String name) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(name), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaObject schema(ImportResult imported, String name) {
        return imported.document().orElseThrow().getComponents().getSchemas().stream()
                .filter(component -> component.getName().equals(name))
                .map(component -> (SchemaObject) component.getSchema())
                .findFirst()
                .orElseThrow();
    }

    private SchemaProperty property(String name) {
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.STRING))
                .build();
        return new SchemaProperty(ElementId.random(), name, schema);
    }
}
