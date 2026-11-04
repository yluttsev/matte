package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.ElementId;
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

class SchemaRenameTest {

    private static final String UTF8_BOM = Character.toString((char) 0xFEFF);

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testRename_WhenYamlContainsReferences_ShouldPreserveUnchangedBytes() throws IOException {
        assertRename("schema-rename.yaml", "schema-renamed.yaml");
    }

    @Test
    void testRename_WhenJsonContainsReferences_ShouldPreserveUnchangedBytes() throws IOException {
        assertRename("schema-rename.json", "schema-renamed.json");
    }

    @Test
    void testRename_WhenYamlNamesSchemaPropertiesExampleAndExtension_ShouldUseNodeRole() throws IOException {
        assertRenameWithOpaqueValues("schema-reference-context.yaml", 6, 7);
    }

    @Test
    void testRename_WhenJsonNamesSchemaPropertiesExampleAndExtension_ShouldUseNodeRole() throws IOException {
        assertRenameWithOpaqueValues("schema-reference-context.json", 6, 7);
    }

    @Test
    void testRename_WhenNewNameContainsPointerSeparator_ShouldEscapeReferenceAndQuoteYamlKey() throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes("schema-rename.yaml"),
                ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "Client/Prime"));

        String text = new String(result.source().orElseThrow().content(), StandardCharsets.UTF_8);
        assertTrue(text.contains("\"Client/Prime\": # keep this comment about Customer"));
        assertTrue(text.contains("$ref: '#/components/schemas/Client~1Prime'"));
        assertTrue(text.contains("$ref: '#/components/schemas/Client~1Prime/properties/friend'"));
        assertTrue(text.contains("https://example.invalid/api.yaml#/components/schemas/Customer"));
    }

    @Test
    void testRename_WhenNewNameIsYamlKeyword_ShouldKeepItAsString() throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes("schema-rename.yaml"),
                ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "null"));

        String text = new String(result.source().orElseThrow().content(), StandardCharsets.UTF_8);
        assertTrue(text.contains("\"null\": # keep this comment about Customer"));
    }

    @Test
    void testRename_WhenNameIsUnchanged_ShouldKeepExactSource() throws IOException {
        byte[] source = TestFixtures.bytes("schema-rename.json");
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "Customer"));

        assertArrayEquals(source, result.source().orElseThrow().content());
    }

    @Test
    void testRename_WhenSourceHasBomAndCrlf_ShouldKeepBoth() throws IOException {
        byte[] original = TestFixtures.bytes("schema-rename.yaml");
        byte[] source = (UTF8_BOM + new String(original, StandardCharsets.UTF_8).replace("\n", "\r\n"))
                .getBytes(StandardCharsets.UTF_8);
        byte[] expected = (UTF8_BOM + new String(TestFixtures.bytes("schema-renamed.yaml"), StandardCharsets.UTF_8)
                .replace("\n", "\r\n")).getBytes(StandardCharsets.UTF_8);
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "Client"));

        assertArrayEquals(expected, result.source().orElseThrow().content());
    }

    @Test
    void testRename_WhenYamlHasBomEmojiAndMixedLineEndings_ShouldPreserveOtherBytes() throws IOException {
        String fixture = new String(TestFixtures.bytes("source-ranges-unicode.yaml"), StandardCharsets.UTF_8);
        String source = Character.toString((char) 0xFEFF) + fixture.replaceFirst("\n", "\r\n");
        ImportResult imported = importer.importDocument(source.getBytes(StandardCharsets.UTF_8),
                ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "New"));

        assertArrayEquals(source.replace("Old:", "New:").getBytes(StandardCharsets.UTF_8),
                result.source().orElseThrow().content());
    }

    @Test
    void testEdit_WhenRenameIsFollowedByAddProperty_ShouldPreserveStateAndElementIds() throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes("schema-rename.yaml"),
                ImportOptions.autoDetect());
        SchemaComponent originalComponent = imported.document().orElseThrow().getComponents().getSchemas().getFirst();
        SchemaObject originalSchema = (SchemaObject) originalComponent.getSchema();
        ElementId existingPropertyId = originalSchema.getProperties().getFirst().getId();

        OpenApiSchemaEditor.RenameResult renamed = editor.rename(imported,
                new RenameSchemaComponent(originalComponent.getId(), "Client"));
        SchemaObject renamedSchema = (SchemaObject) renamed.state().document().orElseThrow()
                .getComponents().getSchemas().getFirst().getSchema();
        OpenApiEditResult added = editor.addProperty(renamed.state(), new AddSchemaProperty(renamedSchema.getId(),
                new SchemaProperty(ElementId.random(), "email",
                        SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.STRING)).build())));
        SchemaObject addedSchema = (SchemaObject) added.state().document().orElseThrow()
                .getComponents().getSchemas().getFirst().getSchema();

        OpenApiEditResult addedAgain = editor.addProperty(added.state(), new AddSchemaProperty(addedSchema.getId(),
                new SchemaProperty(ElementId.random(), "phone",
                        SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.STRING)).build())));

        SchemaObject finalSchema = (SchemaObject) addedAgain.state().document().orElseThrow()
                .getComponents().getSchemas().getFirst().getSchema();
        assertEquals("Client", addedAgain.state().document().orElseThrow()
                .getComponents().getSchemas().getFirst().getName());
        assertEquals(existingPropertyId, finalSchema.getProperties().getFirst().getId());
        assertEquals(4, finalSchema.getProperties().size());
        assertTrue(new String(addedAgain.state().source().content(), StandardCharsets.UTF_8).contains("phone:"));
    }

    @Test
    void testRename_WhenJsonHasUnicodeBeforeSchema_ShouldKeepOffsetsAccurate() throws IOException {
        String prefix = "{\"label\":\"😀\",";
        String sourceText = new String(TestFixtures.bytes("schema-rename.json"), StandardCharsets.UTF_8);
        String expectedText = new String(TestFixtures.bytes("schema-renamed.json"), StandardCharsets.UTF_8);
        String original = prefix + sourceText.substring(1);
        String expected = prefix + expectedText.substring(1);
        ImportResult imported = importer.importDocument(original.getBytes(StandardCharsets.UTF_8),
                ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "Client"));

        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), result.source().orElseThrow().content());
    }

    @Test
    void testRename_WhenJsonHasBom_ShouldPreserveIt() throws IOException {
        byte[] original = (UTF8_BOM + new String(TestFixtures.bytes("schema-rename.json"), StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        byte[] expected = (UTF8_BOM + new String(TestFixtures.bytes("schema-renamed.json"), StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        ImportResult imported = importer.importDocument(original, ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "Client"));

        assertArrayEquals(expected, result.source().orElseThrow().content());
    }

    @Test
    void testRename_WhenTargetAlreadyExists_ShouldNotPatchSource() throws IOException {
        byte[] bytes = TestFixtures.bytes("schema-rename.yaml");
        String text = new String(bytes, StandardCharsets.UTF_8).replace("paths:",
                "    Client:\n      type: string\npaths:");
        ImportResult imported = importer.importDocument(text.getBytes(StandardCharsets.UTF_8),
                ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "Client"));

        assertInstanceOf(EditRejected.class, result.edit());
        assertTrue(result.source().isEmpty());
    }

    @Test
    void testRename_WhenSourceAndModelDisagree_ShouldFailWithoutPartialPatch() throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes("schema-rename.yaml"),
                ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();
        ImportResult mismatched = new ImportResult(new SourceDocument(TestFixtures.bytes("schema-types-3.1.yaml")),
                imported.format(), imported.status(), imported.diagnostics(), imported.version(), imported.document());

        assertThrows(IllegalArgumentException.class, () -> editor.rename(mismatched,
                new RenameSchemaComponent(component.getId(), "Client")));
    }

    @Test
    void testRename_WhenReferenceDiffersFromModel_ShouldRejectPatch() throws IOException {
        byte[] source = TestFixtures.bytes("schema-rename.yaml");
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();
        String changed = new String(source, StandardCharsets.UTF_8)
                .replace("$ref: '#/components/schemas/Customer'", "$ref: '#/components/schemas/Other'");
        ImportResult mismatched = new ImportResult(new SourceDocument(changed.getBytes(StandardCharsets.UTF_8)),
                imported.format(), imported.status(), imported.diagnostics(), imported.version(), imported.document());

        assertThrows(IllegalArgumentException.class, () -> editor.rename(mismatched,
                new RenameSchemaComponent(component.getId(), "Client")));
        assertArrayEquals(source, imported.source().content());
    }

    private void assertRename(String input, String expected) throws IOException {
        byte[] source = TestFixtures.bytes(input);
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "Client"));

        EditSucceeded edit = assertInstanceOf(EditSucceeded.class, result.edit());
        assertTrue(edit.changeSet().changes().stream().anyMatch(ReferenceChanged.class::isInstance));
        assertArrayEquals(source, imported.source().content());
        assertArrayEquals(TestFixtures.bytes(expected), result.source().orElseThrow().content());
        ImportResult checked = importer.importDocument(result.source().orElseThrow().content(),
                ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, checked.status());
        assertEquals("Client", checked.document().orElseThrow().getComponents().getSchemas().getFirst().getName());
    }

    private void assertRenameWithOpaqueValues(
            String fixture,
            int expectedNewReferences,
            int expectedOldLiterals
    )
            throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(fixture), ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        OpenApiSchemaEditor.RenameResult result = editor.rename(imported,
                new RenameSchemaComponent(component.getId(), "New"));

        String patched = new String(result.source().orElseThrow().content(), StandardCharsets.UTF_8);
        assertEquals(expectedNewReferences, count(patched, "#/components/schemas/New"), patched);
        assertEquals(expectedOldLiterals, count(patched, "#/components/schemas/Old"));
        assertTrue(patched.contains("alias: New") || patched.contains("\"alias\": \"New\""));
    }

    private int count(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
