package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.List;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.patch.OpenApiSchemaDeletionPatcher;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscriminatorMappingRefKeyTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testRename_WhenDiscriminatorMappingKeyIsRef_ShouldRelocateShortNameAndUri() throws IOException {
        for (String extension : new String[] {"yaml", "json"}) {
            ImportResult imported = importFixture("schema-discriminator-ref-mapping." + extension);
            SchemaComponent cat = component(imported, "Cat");

            OpenApiSchemaEditor.RenameResult result = editor.rename(
                    imported,
                    new RenameSchemaComponent(cat.getId(), "Feline")
            );

            assertInstanceOf(EditSucceeded.class, result.edit());
            assertArrayEquals(
                    TestFixtures.bytes("schema-discriminator-ref-mapping-renamed." + extension),
                    result.source().orElseThrow().content()
            );
            assertEquals("Feline", component(result.state(), "Feline").getName());
        }
    }

    @Test
    void testDeleteComponent_WhenDiscriminatorMappingRefKeyTargetsIt_ShouldRejectForShortNameAndUri()
            throws IOException {
        for (String extension : new String[] {"yaml", "json"}) {
            ImportResult imported = importFixture("schema-discriminator-ref-mapping." + extension);
            SchemaComponent cat = component(imported, "Cat");
            byte[] originalSource = imported.source().content();

            OpenApiEditResult result = editor.deleteComponent(imported, new DeleteSchemaComponent(cat.getId()));
            List<DocumentPath> incoming = new OpenApiSchemaDeletionPatcher().incomingReferences(
                    imported.source(),
                    imported.format().orElseThrow(),
                    DocumentPath.schemaComponent("Cat")
            );

            assertInstanceOf(EditRejected.class, result.edit());
            assertTrue(result.source().isEmpty());
            assertArrayEquals(originalSource, imported.source().content());
            assertTrue(incoming.contains(DocumentPath.parse(
                    "/components/schemas/Animal/discriminator/mapping/$ref"
            )));
            assertTrue(incoming.contains(DocumentPath.parse(
                    "/components/schemas/Animal/discriminator/mapping/direct"
            )));
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
