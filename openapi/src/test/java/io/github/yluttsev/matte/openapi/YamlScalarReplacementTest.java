package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static io.github.yluttsev.matte.openapi.testing.EditAssertions.assertRefused;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class YamlScalarReplacementTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @Test
    void testUpdateSchema_WhenLiteralScalarHasFollowingKey_ShouldPreserveLineBoundary() throws IOException {
        assertDescriptionReplacement("Literal", "yaml-scalar-replacement-literal.yaml", false);
    }

    @Test
    void testUpdateSchema_WhenFoldedScalarUsesCrLf_ShouldPreserveLineBoundary() throws IOException {
        assertDescriptionReplacement("Folded", "yaml-scalar-replacement-folded.yaml", true);
    }

    @Test
    void testUpdateSchema_WhenDefaultIsEmptyScalar_ShouldInsertSpaceAfterColon() throws IOException {
        assertEmptyReplacement(false);
    }

    @Test
    void testUpdateSchema_WhenDefaultIsEmptyScalarWithCrLf_ShouldInsertSpaceAfterColon() throws IOException {
        assertEmptyReplacement(true);
    }

    @Test
    void testUpdateSchema_WhenEmptyScalarHasComment_ShouldKeepOriginalState() throws IOException {
        byte[] source = fixture("yaml-scalar-replacement-empty-comment.yaml", false);
        ImportResult imported = importSource(source);
        SchemaObject original = schema(imported, "Empty");
        SchemaObject replacement = SchemaObject.builder(original)
                .defaultValue(new StringValue("Updated"))
                .build();

        assertRefused(imported, () -> editor.updateSchema(imported, new UpdateSchemaObject(replacement)));

        assertArrayEquals(source, imported.source().content());
        assertEquals(original.getDefaultValue(), schema(imported, "Empty").getDefaultValue());
    }

    private void assertEmptyReplacement(boolean crlf) throws IOException {
        ImportResult imported = importSource(fixture("yaml-scalar-replacement.yaml", crlf));
        SchemaObject original = schema(imported, "Empty");
        SchemaObject replacement = SchemaObject.builder(original)
                .defaultValue(new StringValue("Updated"))
                .build();

        OpenApiEditResult edited = editor.updateSchema(imported, new UpdateSchemaObject(replacement));

        assertResult(imported, edited, "yaml-scalar-replacement-empty.yaml", crlf);
        assertEquals(new StringValue("Updated"), schema(edited.state(), "Empty").getDefaultValue());
    }

    @Test
    void testUpdateSchema_WhenIndicatorHasComment_ShouldKeepOriginalState() throws IOException {
        byte[] source = fixture("yaml-scalar-replacement-comment.yaml", false);
        ImportResult imported = importSource(source);
        SchemaObject original = schema(imported, "Literal");
        SchemaObject replacement = SchemaObject.builder(original).description("Updated").build();

        assertRefused(imported, () -> editor.updateSchema(imported, new UpdateSchemaObject(replacement)));

        assertArrayEquals(source, imported.source().content());
        assertEquals(original.getDescription(), schema(imported, "Literal").getDescription());
    }

    private void assertDescriptionReplacement(String name, String expected, boolean crlf) throws IOException {
        ImportResult imported = importSource(fixture("yaml-scalar-replacement.yaml", crlf));
        SchemaObject replacement = SchemaObject.builder(schema(imported, name)).description("Updated").build();

        OpenApiEditResult edited = editor.updateSchema(imported, new UpdateSchemaObject(replacement));

        assertResult(imported, edited, expected, crlf);
        assertEquals("Updated", schema(edited.state(), name).getDescription());
    }

    private void assertResult(ImportResult imported, OpenApiEditResult edited, String expected, boolean crlf)
            throws IOException {
        assertInstanceOf(OpenApiEditResult.Applied.class, edited);
        assertArrayEquals(fixture(expected, crlf), edited.source().orElseThrow().content());
        assertArrayEquals(fixture("yaml-scalar-replacement.yaml", crlf), imported.source().content());
        ImportResult reimported = importer.importDocument(
                edited.source().orElseThrow().content(),
                ImportOptions.autoDetect()
        );
        assertEquals(ImportStatus.IMPORTED, reimported.status(), reimported.diagnostics().toString());
    }

    private byte[] fixture(String name, boolean crlf) throws IOException {
        byte[] bytes = TestFixtures.bytes(name);
        if (!crlf) {
            return bytes;
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        return text.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private ImportResult importSource(byte[] source) {
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaObject schema(ImportResult imported, String name) {
        return assertInstanceOf(
                SchemaObject.class,
                imported.document().orElseThrow().getComponents().getSchemas().stream()
                        .filter(component -> component.getName().equals(name))
                        .findFirst().orElseThrow().getSchema()
        );
    }
}
