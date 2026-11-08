package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static io.github.yluttsev.matte.openapi.testing.EditAssertions.assertRefused;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaReferenceContextSafetyTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @Test
    void testRename_WhenUnknownSchemaFieldContainsTargetRef_ShouldRefuseWithoutChangingState()
            throws IOException {
        for (String extension : new String[] {"yaml", "json"}) {
            ImportResult imported = importFixture("schema-reference-unknown-keyword." + extension);
            SchemaComponent old = component(imported, "Old");

            OpenApiEditResult.Refused refusal = assertRefused(imported, () -> editor.rename(
                    imported,
                    new RenameSchemaComponent(old.getId(), "New")
            ));

            assertTrue(refusal.message().contains("custom-keyword"));
            assertArrayEquals(TestFixtures.bytes("schema-reference-unknown-keyword." + extension),
                    imported.source().content());
            assertEquals("Old", component(imported, "Old").getName());
        }
    }

    @Test
    void testDeleteComponent_WhenOnlyUnknownFieldRefTargetsIt_ShouldRejectWithoutChangingState()
            throws IOException {
        for (String extension : new String[] {"yaml", "json"}) {
            ImportResult imported = importFixture("schema-reference-unknown-only." + extension);
            SchemaComponent old = component(imported, "Old");

            OpenApiEditResult result = editor.deleteComponent(imported, new DeleteSchemaComponent(old.getId()));
            EditRejected rejected = assertInstanceOf(OpenApiEditResult.Rejected.class, result).edit();
            assertTrue(rejected.issues().getFirst().message().contains("custom-keyword"));
            assertSame(imported, result.state());
            assertTrue(result.source().isEmpty());
            assertArrayEquals(TestFixtures.bytes("schema-reference-unknown-only." + extension),
                    imported.source().content());
            assertEquals("Old", component(imported, "Old").getName());
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
