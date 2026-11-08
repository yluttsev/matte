package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.List;
import io.github.yluttsev.matte.core.editing.EditCode;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static io.github.yluttsev.matte.openapi.testing.EditAssertions.assertRefused;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaStructuralEditingTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @Test
    void testRenameProperty_WhenYamlAndJsonHaveDirectPointers_ShouldUpdateRequiredAndSourcePointers() throws IOException {
        for (String extension : List.of("yaml", "json")) {
            ImportResult imported = importFixture("schema-structural-editing-3.1." + extension);
            SchemaObject customer = schema(imported.document().orElseThrow(), "Customer");
            SchemaProperty property = property(customer, "id");

            OpenApiEditResult result = editor.renameProperty(
                    imported,
                    new RenameSchemaProperty(customer.getId(), property.getId(), "identifier")
            );

            assertInstanceOf(OpenApiEditResult.Applied.class, result);
            assertArrayEquals(
                    TestFixtures.bytes("schema-structural-editing-3.1-renamed." + extension),
                    result.source().orElseThrow().content()
            );
            SchemaObject renamed = schema(result.state().document().orElseThrow(), "Customer");
            assertEquals(property.getId(), property(renamed, "identifier").getId());
            assertEquals(List.of("identifier"), renamed.getRequired().stream().toList());
        }
    }

    @Test
    void testDeleteProperty_WhenUnreferenced_ShouldRemoveOnlyPropertyAndPreserveOtherSource() throws IOException {
        for (String extension : List.of("yaml", "json")) {
            ImportResult imported = importFixture("schema-structural-editing-3.1." + extension);
            SchemaObject customer = schema(imported.document().orElseThrow(), "Customer");
            SchemaProperty property = property(customer, "unused");

            OpenApiEditResult result = editor.deleteProperty(
                    imported,
                    new DeleteSchemaProperty(customer.getId(), property.getId())
            );

            assertInstanceOf(OpenApiEditResult.Applied.class, result);
            assertArrayEquals(
                    TestFixtures.bytes("schema-structural-editing-3.1-deleted." + extension),
                    result.source().orElseThrow().content()
            );
            assertTrue(schema(result.state().document().orElseThrow(), "Customer").getProperties().stream()
                    .noneMatch(candidate -> candidate.getName().equals("unused")));
        }
    }

    @Test
    void testDeleteProperty_WhenRemovingLastProperty_ShouldPreserveEmptyPropertiesSection() throws IOException {
        ImportResult imported = importFixture("schema-property-removal.yaml");
        SchemaObject schema = schema(imported.document().orElseThrow(), "Empty");
        SchemaProperty property = property(schema, "obsolete");

        OpenApiEditResult result = editor.deleteProperty(
                imported,
                new DeleteSchemaProperty(schema.getId(), property.getId())
        );

        assertInstanceOf(OpenApiEditResult.Applied.class, result);
        assertArrayEquals(
                TestFixtures.bytes("schema-property-removal-empty.yaml"),
                result.source().orElseThrow().content()
        );
        assertTrue(schema(result.state().document().orElseThrow(), "Empty").getProperties().isEmpty());
    }

    @Test
    void testDeleteComponent_WhenRemovingLastComponent_ShouldPreserveEmptySchemasSection() throws IOException {
        ImportResult imported = importFixture("schema-component-removal.yaml");
        SchemaComponent component = component(imported.document().orElseThrow(), "Empty");

        OpenApiEditResult result = editor.deleteComponent(imported, new DeleteSchemaComponent(component.getId()));

        assertInstanceOf(OpenApiEditResult.Applied.class, result);
        assertArrayEquals(
                TestFixtures.bytes("schema-component-removal-empty.yaml"),
                result.source().orElseThrow().content()
        );
        assertTrue(result.state().document().orElseThrow().getComponents().getSchemas().isEmpty());
    }

    @Test
    void testDeleteComponent_WhenReferencedOnlyFromPaths_ShouldRejectAndKeepOriginalState() throws IOException {
        ImportResult imported = importFixture("schema-component-referenced-from-paths.yaml");
        SchemaComponent component = component(imported.document().orElseThrow(), "Empty");
        byte[] source = imported.source().content();

        OpenApiEditResult result = editor.deleteComponent(imported, new DeleteSchemaComponent(component.getId()));

        EditRejected rejected = assertInstanceOf(OpenApiEditResult.Rejected.class, result).edit();
        assertEquals(EditCode.REFERENCED_TARGET, rejected.issues().getFirst().code());
        assertTrue(rejected.issues().getFirst().message().contains("$ref"));
        assertArrayEquals(source, imported.source().content());
        assertSame(imported, result.state());
        assertTrue(result.source().isEmpty());
    }

    @Test
    void testDeleteProperty_WhenLeadingYamlCommentIsAmbiguous_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("schema-property-removal-commented.yaml");
        SchemaObject schema = schema(imported.document().orElseThrow(), "Empty");
        SchemaProperty property = property(schema, "obsolete");
        byte[] source = imported.source().content();

        OpenApiEditResult.Refused error = assertRefused(
                imported,
                () -> editor.deleteProperty(imported, new DeleteSchemaProperty(schema.getId(), property.getId())));

        assertTrue(error.message().contains("comments"));
        assertArrayEquals(source, imported.source().content());
    }

    @Test
    void testDeleteProperty_WhenCommentIsInsideDeletedNode_ShouldDeleteCommentWithNode() throws IOException {
        ImportResult imported = importFixture("schema-property-removal-internal-commented.yaml");
        SchemaObject schema = schema(imported.document().orElseThrow(), "Empty");
        SchemaProperty property = property(schema, "obsolete");

        OpenApiEditResult result = editor.deleteProperty(
                imported,
                new DeleteSchemaProperty(schema.getId(), property.getId())
        );

        assertInstanceOf(OpenApiEditResult.Applied.class, result);
        assertArrayEquals(
                TestFixtures.bytes("schema-property-removal-empty.yaml"),
                result.source().orElseThrow().content()
        );
    }

    @Test
    void testDeleteProperty_WhenFlowMappingCommentIsAmbiguous_ShouldRejectWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("schema-property-removal-flow-commented.yaml");
        SchemaObject schema = schema(imported.document().orElseThrow(), "Empty");
        SchemaProperty property = property(schema, "obsolete");
        byte[] source = imported.source().content();

        OpenApiEditResult.Refused error = assertRefused(
                imported,
                () -> editor.deleteProperty(imported, new DeleteSchemaProperty(schema.getId(), property.getId())));

        assertTrue(error.message().contains("Comments"));
        assertArrayEquals(source, imported.source().content());
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

    private SchemaObject schema(ApiDocument document, String name) {
        return (SchemaObject) component(document, name).getSchema();
    }

    private SchemaProperty property(SchemaObject schema, String name) {
        return schema.getProperties().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst().orElseThrow();
    }
}
