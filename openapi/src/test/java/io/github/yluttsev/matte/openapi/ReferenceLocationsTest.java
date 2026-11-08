package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import io.github.yluttsev.matte.core.editing.EditCode;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.patch.OpenApiSchemaDeletionPatcher;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceLocationsTest {

    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @ParameterizedTest
    @CsvSource({"3.0,yaml", "3.0,json", "3.1,yaml", "3.1,json"})
    void testRename_WhenCatalogueContainsAllReferenceRoles_ShouldRelocateOnlyReferences(
            String version,
            String extension
    )
            throws IOException {
        ImportResult imported = importCatalogue(version, extension);
        byte[] original = imported.source().content();

        OpenApiSchemaEditor.RenameResult result = editor.rename(
                imported,
                new RenameSchemaComponent(component(imported).getId(), "New")
        );

        assertInstanceOf(EditSucceeded.class, result.edit());
        assertArrayEquals(TestFixtures.bytes("reference-locations-" + version + "-renamed." + extension),
                result.source().orElseThrow().content());
        assertArrayEquals(original, imported.source().content());
        List<DocumentPath> remaining = new OpenApiSchemaDeletionPatcher().incomingReferences(
                result.source().orElseThrow(), imported.format().orElseThrow(), DocumentPath.schemaComponent("Old")
        );
        assertTrue(remaining.isEmpty(), remaining.toString());
    }

    @ParameterizedTest
    @CsvSource({"3.0,yaml", "3.0,json", "3.1,yaml", "3.1,json"})
    void testDelete_WhenCatalogueContainsIncomingReferences_ShouldListEveryLocationAndPreserveState(
            String version,
            String extension
    )
            throws IOException {
        ImportResult imported = importCatalogue(version, extension);
        byte[] original = imported.source().content();
        List<String> expected = new String(TestFixtures.bytes("reference-locations-" + version + ".txt"),
                StandardCharsets.UTF_8).lines().toList();
        List<DocumentPath> actual = new OpenApiSchemaDeletionPatcher().incomingReferences(
                imported.source(), imported.format().orElseThrow(), DocumentPath.schemaComponent("Old")
        );
        assertEquals(new HashSet<>(expected), new HashSet<>(actual.stream().map(Object::toString).toList()));

        OpenApiEditResult result = editor.deleteComponent(
                imported, new DeleteSchemaComponent(component(imported).getId())
        );

        EditRejected rejected = assertInstanceOf(EditRejected.class, result.edit());
        assertEquals(EditCode.REFERENCED_TARGET, rejected.issues().getFirst().code());
        for (String location : expected) {
            assertTrue(rejected.issues().getFirst().message().contains(location), location);
        }
        assertSame(imported, result.state());
        assertTrue(result.source().isEmpty());
        assertArrayEquals(original, imported.source().content());
        assertEquals("Old", component(imported).getName());
    }

    private ImportResult importCatalogue(String version, String extension) throws IOException {
        return importFixture("reference-locations-" + version + "." + extension);
    }

    @ParameterizedTest
    @ValueSource(strings = {"yaml", "json"})
    void testRenameProperty_WhenReferencesAreInUnmappedSections_ShouldRelocateAllAndRejectDeletion(String extension)
            throws IOException {
        ImportResult imported = importFixture("reference-property-locations." + extension);
        SchemaObject schema = (SchemaObject) component(imported).getSchema();
        var property = schema.getProperties().getFirst();
        byte[] original = imported.source().content();

        OpenApiEditResult deletion = editor.deleteProperty(
                imported, new DeleteSchemaProperty(schema.getId(), property.getId())
        );
        EditRejected rejected = assertInstanceOf(EditRejected.class, deletion.edit());
        for (String location : List.of("/webhooks/", "/components/callbacks/", "/components/pathItems/")) {
            assertTrue(rejected.issues().getFirst().message().contains(location), location);
        }
        assertSame(imported, deletion.state());
        assertTrue(deletion.source().isEmpty());

        OpenApiEditResult rename = editor.renameProperty(
                imported, new RenameSchemaProperty(schema.getId(), property.getId(), "identifier")
        );
        assertInstanceOf(EditSucceeded.class, rename.edit());
        assertArrayEquals(TestFixtures.bytes("reference-property-locations-renamed." + extension),
                rename.source().orElseThrow().content());
        assertArrayEquals(original, imported.source().content());
    }

    @ParameterizedTest
    @ValueSource(strings = {"yaml", "json"})
    void testEdit_WhenOnlyLiteralsAndUnrelatedUnknownReferencesExist_ShouldPreserveTheirBytes(String extension)
            throws IOException {
        ImportResult imported = importFixture("reference-literals-only." + extension);

        OpenApiSchemaEditor.RenameResult rename = editor.rename(
                imported, new RenameSchemaComponent(component(imported).getId(), "New")
        );
        assertInstanceOf(EditSucceeded.class, rename.edit());
        assertArrayEquals(TestFixtures.bytes("reference-literals-only-renamed." + extension),
                rename.source().orElseThrow().content());

        OpenApiEditResult deletion = editor.deleteComponent(
                imported, new DeleteSchemaComponent(component(imported).getId())
        );
        assertInstanceOf(EditSucceeded.class, deletion.edit());
        assertArrayEquals(TestFixtures.bytes("reference-literals-only-deleted." + extension),
                deletion.source().orElseThrow().content());
    }

    @ParameterizedTest
    @ValueSource(strings = {"yaml", "json"})
    void testEdit_WhenMultipleUnknownReferencesTargetSubtree_ShouldRefuseAndListAllLocations(String extension)
            throws IOException {
        ImportResult imported = importFixture("reference-unknown-multiple." + extension);
        byte[] original = imported.source().content();
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class, () -> editor.rename(
                imported, new RenameSchemaComponent(component(imported).getId(), "New")
        ));
        OpenApiEditResult deletion = editor.deleteComponent(
                imported, new DeleteSchemaComponent(component(imported).getId())
        );
        EditRejected rejected = assertInstanceOf(EditRejected.class, deletion.edit());
        List<DocumentPath> incoming = new OpenApiSchemaDeletionPatcher().incomingReferences(
                imported.source(), imported.format().orElseThrow(), DocumentPath.schemaComponent("Old")
        );
        assertEquals(3, incoming.size());
        for (String location : List.of("/one/$ref", "/two/$ref", "/lookalike/components/schemas/Fake/$ref")) {
            assertTrue(refusal.getMessage().contains(location), location);
            assertTrue(rejected.issues().getFirst().message().contains(location), location);
        }
        assertSame(imported, deletion.state());
        assertTrue(deletion.source().isEmpty());
        assertArrayEquals(original, imported.source().content());
        assertEquals("Old", component(imported).getName());
    }

    private ImportResult importFixture(String name) throws IOException {
        ImportResult imported = new OpenApiImporter().importDocument(
                TestFixtures.bytes(name), ImportOptions.autoDetect()
        );
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaComponent component(ImportResult imported) {
        return imported.document().orElseThrow().getComponents().getSchemas().stream()
                .filter(candidate -> candidate.getName().equals("Old"))
                .findFirst().orElseThrow();
    }
}
