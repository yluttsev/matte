package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static io.github.yluttsev.matte.openapi.testing.EditAssertions.assertRefused;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlOnlyFieldRemovalTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @Test
    void testDeleteProperty_WhenOnlyBlockEntryPrecedesAnotherSchema_ShouldPreserveNextKey() throws IOException {
        ImportResult imported = importFixture("yaml-only-property-middle.yaml");
        SchemaObject schema = schema(imported);
        SchemaProperty property = schema.getProperties().getFirst();

        OpenApiEditResult edited = editor.deleteProperty(
                imported,
                new DeleteSchemaProperty(schema.getId(), property.getId())
        );

        assertRoundTrip(imported, edited, "yaml-only-property-middle.yaml", "yaml-only-property-middle-removed.yaml");
        assertTrue(schema(edited.state()).getProperties().isEmpty());
    }

    @Test
    void testDeleteComponent_WhenOnlyBlockEntryPrecedesPaths_ShouldPreserveNextKey() throws IOException {
        ImportResult imported = importFixture("yaml-only-component-middle.yaml");
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiEditResult edited = editor.deleteComponent(imported, new DeleteSchemaComponent(component.getId()));

        assertRoundTrip(imported, edited, "yaml-only-component-middle.yaml", "yaml-only-component-middle-removed.yaml");
        assertTrue(edited.state().document().orElseThrow().getComponents().getSchemas().isEmpty());
    }

    @Test
    void testUpdateSchema_WhenOnlyFieldPrecedesAnotherSchema_ShouldPreserveNextKey() throws IOException {
        assertFieldRemoval("yaml-only-field-middle.yaml", "yaml-only-field-middle-removed.yaml");
    }

    @Test
    void testUpdateSchema_WhenOnlyFieldEndsDocument_ShouldPreserveTrailingNewline() throws IOException {
        assertFieldRemoval("yaml-only-field-end.yaml", "yaml-only-field-end-removed.yaml");
    }

    @Test
    void testDeleteProperty_WhenTrailingCommentIsAmbiguous_ShouldKeepOriginalState() throws IOException {
        ImportResult imported = importFixture("yaml-only-property-middle-comment.yaml");
        SchemaObject schema = schema(imported);
        SchemaProperty property = schema.getProperties().getFirst();
        byte[] source = imported.source().content();

        assertRefused(
                imported,
                () -> editor.deleteProperty(imported, new DeleteSchemaProperty(schema.getId(), property.getId())));

        assertArrayEquals(source, imported.source().content());
        assertEquals(property.getId(), schema(imported).getProperties().getFirst().getId());
    }

    private void assertFieldRemoval(String input, String expected) throws IOException {
        ImportResult imported = importFixture(input);
        SchemaObject replacement = SchemaObject.builder(schema(imported)).minLength(null).build();

        OpenApiEditResult edited = editor.updateSchema(imported, new UpdateSchemaObject(replacement));

        assertRoundTrip(imported, edited, input, expected);
        assertNull(schema(edited.state()).getMinLength());
    }

    private void assertRoundTrip(
            ImportResult imported,
            OpenApiEditResult edited,
            String input,
            String expected
    )
            throws IOException {
        assertInstanceOf(OpenApiEditResult.Applied.class, edited);
        assertArrayEquals(TestFixtures.bytes(expected), edited.source().orElseThrow().content());
        assertArrayEquals(TestFixtures.bytes(input), imported.source().content());
        ImportResult reimported = importer.importDocument(
                edited.source().orElseThrow().content(),
                ImportOptions.autoDetect()
        );
        assertEquals(ImportStatus.IMPORTED, reimported.status(), reimported.diagnostics().toString());
    }

    private ImportResult importFixture(String name) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(name), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaObject schema(ImportResult imported) {
        return assertInstanceOf(
                SchemaObject.class,
                imported.document().orElseThrow().getComponents().getSchemas().getFirst().getSchema()
        );
    }
}
