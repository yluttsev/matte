package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import io.github.yluttsev.matte.core.editing.EditCode;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentNameValidationTest {

    private static final String VALID_NAME = "Valid.Name-123_";

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @ParameterizedTest
    @ValueSource(strings = {"3.0.yaml", "3.0.json", "3.1.yaml", "3.1.json"})
    void testRename_WhenImportedNameIsInvalid_ShouldRepairNameAndReference(String fixture) throws IOException {
        byte[] source = TestFixtures.bytes("component-name-" + fixture);
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();
        assertEquals("Legacy/Name", component.getName());

        OpenApiEditResult.Applied result = assertInstanceOf(OpenApiEditResult.Applied.class, editor.rename(
                imported, new RenameSchemaComponent(component.getId(), VALID_NAME)
        ));

        String expected = "component-name-" + fixture.replace(".yaml", "-renamed.yaml")
                .replace(".json", "-renamed.json");
        assertArrayEquals(TestFixtures.bytes(expected), result.source().orElseThrow().content());
        assertArrayEquals(source, imported.source().content());
        SchemaComponent renamed = result.state().document().orElseThrow().getComponents().getSchemas().getFirst();
        assertEquals(VALID_NAME, renamed.getName());
        assertEquals(component.getId(), renamed.getId());
        assertEquals("a/b~# имя", ((SchemaObject) renamed.getSchema()).getProperties().getFirst().getName());
        assertEquals(ImportStatus.IMPORTED, importer.importDocument(
                result.source().orElseThrow().content(), ImportOptions.autoDetect()
        ).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.yaml", "3.0.json", "3.1.yaml", "3.1.json"})
    void testApply_WhenComponentNameIsInvalid_ShouldRejectAndPreserveSource(String fixture) throws IOException {
        byte[] source = TestFixtures.bytes("component-name-" + fixture);
        ImportResult imported = importer.importDocument(source, ImportOptions.autoDetect());
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        for (String name : new String[] {"", " ", ". space", "/", "~", "#", "имя"}) {
            SchemaComponent candidate = new SchemaComponent(
                    ElementId.random(), name, SchemaObject.builder(ElementId.random()).build()
            );
            assertRejected(imported, editor.addComponent(imported, new AddSchemaComponent(candidate)));
            assertRejected(imported, editor.rename(imported, new RenameSchemaComponent(component.getId(), name)));
        }

        assertArrayEquals(source, imported.source().content());
        SchemaComponent unchanged = imported.document().orElseThrow().getComponents().getSchemas().getFirst();
        assertEquals("Legacy/Name", unchanged.getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.yaml", "3.0.json", "3.1.yaml", "3.1.json"})
    void testApply_WhenComponentNameMatchesPattern_ShouldProduceImportableSource(String fixture) throws IOException {
        ImportResult imported = importer.importDocument(
                TestFixtures.bytes("component-name-" + fixture), ImportOptions.autoDetect()
        );
        SchemaComponent component = imported.document().orElseThrow().getComponents().getSchemas().getFirst();

        for (String name : new String[] {".", "-", "_", "0", "123", "a", "Z", VALID_NAME}) {
            SchemaComponent candidate = new SchemaComponent(
                    ElementId.random(), name, SchemaObject.builder(ElementId.random()).build()
            );
            OpenApiEditResult.Applied added = assertInstanceOf(OpenApiEditResult.Applied.class,
                    editor.addComponent(imported, new AddSchemaComponent(candidate)));
            OpenApiEditResult.Applied renamed = assertInstanceOf(OpenApiEditResult.Applied.class,
                    editor.rename(imported, new RenameSchemaComponent(component.getId(), name)));
            for (OpenApiEditResult.Applied result : new OpenApiEditResult.Applied[] {added, renamed}) {
                ImportResult reimported = importer.importDocument(
                        result.source().orElseThrow().content(), ImportOptions.autoDetect()
                );
                assertEquals(ImportStatus.IMPORTED, reimported.status());
                assertTrue(reimported.document().orElseThrow().getComponents().getSchemas().stream()
                        .anyMatch(schema -> schema.getName().equals(name)));
            }
        }
    }

    private static void assertRejected(ImportResult imported, OpenApiEditResult result) {
        OpenApiEditResult.Rejected rejected = assertInstanceOf(OpenApiEditResult.Rejected.class, result);
        assertEquals(EditCode.INVALID_NAME, rejected.edit().issues().getFirst().code());
        assertSame(imported, rejected.state());
        assertTrue(rejected.source().isEmpty());
    }
}
