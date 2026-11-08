package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class YamlFlowAdditionTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @Test
    void testUpdateSchema_WhenLastFieldIsMultilineFlowArray_ShouldInsertAfterClosingBracket() throws IOException {
        assertAdded("yaml-flow-array-last.yaml", "yaml-flow-array-last-added.yaml", false);
    }

    @Test
    void testUpdateSchema_WhenLastFieldIsMultilineFlowMapping_ShouldInsertAfterClosingBrace() throws IOException {
        assertAdded("yaml-flow-map-last.yaml", "yaml-flow-map-last-added.yaml", false);
    }

    @Test
    void testUpdateSchema_WhenMultilineFlowMappingUsesCrLf_ShouldPreserveLineEndings() throws IOException {
        assertAdded("yaml-flow-map-last.yaml", "yaml-flow-map-last-added.yaml", true);
    }

    private void assertAdded(String input, String expected, boolean crlf) throws IOException {
        byte[] source = fixture(input, crlf);
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        SchemaObject replacement = SchemaObject.builder(schema(imported)).title("AddedTitle").build();

        OpenApiEditResult edited = editor.updateSchema(imported, new UpdateSchemaObject(replacement));

        assertInstanceOf(OpenApiEditResult.Applied.class, edited);
        assertArrayEquals(fixture(expected, crlf), edited.source().orElseThrow().content());
        assertArrayEquals(source, imported.source().content());
        assertEquals("AddedTitle", schema(edited.state()).getTitle());
        ImportResult reimported = importer.importDocument(
                edited.source().orElseThrow().content(),
                ImportOptions.autoDetect()
        );
        assertEquals(ImportStatus.IMPORTED, reimported.status(), reimported.diagnostics().toString());
        assertEquals("AddedTitle", schema(reimported).getTitle());
    }

    private byte[] fixture(String name, boolean crlf) throws IOException {
        byte[] bytes = TestFixtures.bytes(name);
        if (!crlf) {
            return bytes;
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        return text.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private SchemaObject schema(ImportResult imported) {
        return assertInstanceOf(
                SchemaObject.class,
                imported.document().orElseThrow().getComponents().getSchemas().getFirst().getSchema()
        );
    }
}
