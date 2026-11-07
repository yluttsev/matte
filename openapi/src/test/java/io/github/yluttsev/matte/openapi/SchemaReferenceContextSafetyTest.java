package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaReferenceContextSafetyTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testRename_WhenUnknownSchemaFieldContainsRef_ShouldPreserveItAndUpdateKnownReferences()
            throws IOException {
        for (String extension : new String[] {"yaml", "json"}) {
            ImportResult imported = importFixture("schema-reference-unknown-keyword." + extension);
            SchemaComponent old = component(imported, "Old");

            OpenApiSchemaEditor.RenameResult result = editor.rename(
                    imported,
                    new RenameSchemaComponent(old.getId(), "New")
            );

            assertInstanceOf(EditSucceeded.class, result.edit());
            assertArrayEquals(
                    TestFixtures.bytes("schema-reference-unknown-keyword-renamed." + extension),
                    result.source().orElseThrow().content()
            );
            assertEquals("New", component(result.state(), "New").getName());
        }
    }

    @Test
    void testDeleteComponent_WhenOnlyUnknownFieldRefTargetsIt_ShouldDeleteWithoutChangingUnknownValue()
            throws IOException {
        for (String extension : new String[] {"yaml", "json"}) {
            ImportResult imported = importFixture("schema-reference-unknown-only." + extension);
            SchemaComponent old = component(imported, "Old");

            OpenApiEditResult result = editor.deleteComponent(imported, new DeleteSchemaComponent(old.getId()));
            String text = new String(result.source().orElseThrow().content(), StandardCharsets.UTF_8);

            assertInstanceOf(EditSucceeded.class, result.edit());
            if (extension.equals("yaml")) {
                assertFalse(text.contains("    Old:\n"));
            } else {
                assertFalse(text.contains("\"Old\": {"));
            }
            assertTrue(text.contains("#/components/schemas/Old"));
            assertEquals(1, result.state().document().orElseThrow().getComponents().getSchemas().size());
        }
    }

    private ImportResult importFixture(String name) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(name), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaComponent component(ImportResult imported, String name) {
        return imported.document().orElseThrow().getComponents().getSchemas().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
