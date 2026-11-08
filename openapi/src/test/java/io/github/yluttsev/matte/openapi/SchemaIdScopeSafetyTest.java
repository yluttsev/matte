package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaIdScopeSafetyTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @Test
    void testRename_WhenAbsoluteAndNestedRelativeIdsScopeReferences_ShouldRejectAtomically() throws IOException {
        for (String extension : List.of("yaml", "json")) {
            ImportResult imported = importFixture("schema-id-scope." + extension);
            SchemaComponent component = component(imported.document().orElseThrow(), "A");
            byte[] original = imported.source().content();

            OpenApiEditResult result = editor.rename(
                    imported,
                    new RenameSchemaComponent(component.getId(), "Renamed")
            );

            OpenApiEditResult.Refused rejection = assertInstanceOf(OpenApiEditResult.Refused.class, result);
            assertEquals(SourceEditCode.SCHEMA_ID_SCOPE, rejection.code());
            assertTrue(rejection.message().contains("$id"));
            assertArrayEquals(original, imported.source().content());
            assertSame(imported, result.state());
            assertTrue(result.source().isEmpty());
            assertEquals("A", component(result.state().document().orElseThrow(), "A").getName());
        }
    }

    @Test
    void testDeleteComponent_WhenSchemaIdsMakeReferenceScopeUncertain_ShouldRejectAtomically() throws IOException {
        ImportResult imported = importFixture("schema-id-delete.yaml");
        SchemaComponent component = component(imported.document().orElseThrow(), "Canonical");
        byte[] original = imported.source().content();

        OpenApiEditResult result = editor.deleteComponent(imported, new DeleteSchemaComponent(component.getId()));

        OpenApiEditResult.Refused rejection = assertInstanceOf(OpenApiEditResult.Refused.class, result);
        assertEquals(SourceEditCode.SCHEMA_ID_SCOPE, rejection.code());
        assertTrue(rejection.message().contains("$id"));
        assertArrayEquals(original, imported.source().content());
        assertSame(imported, result.state());
        assertTrue(result.source().isEmpty());
        assertEquals("Canonical", component(result.state().document().orElseThrow(), "Canonical").getName());
    }

    @Test
    void testRename_WhenOnlyAnchorAndExtensionDataUseId_ShouldRelocateRootPointer() throws IOException {
        ImportResult imported = importFixture("schema-anchor-only.yaml");
        SchemaComponent component = component(imported.document().orElseThrow(), "A");

        OpenApiEditResult result = editor.rename(
                imported,
                new RenameSchemaComponent(component.getId(), "Renamed")
        );

        assertInstanceOf(OpenApiEditResult.Applied.class, result);
        String updated = new String(result.source().orElseThrow().content(), StandardCharsets.UTF_8);
        assertTrue(updated.contains("$anchor: marker"));
        assertTrue(updated.contains("$id: https://example.com/extension.json"));
        assertTrue(updated.contains("$ref: \"#/components/schemas/A\""));
        assertTrue(updated.contains("$ref: \"#/components/schemas/Renamed\""));
    }

    private ImportResult importFixture(String fixtureName) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(fixtureName), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaComponent component(ApiDocument document, String name) {
        return document.getComponents().getSchemas().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst().orElseThrow();
    }
}
