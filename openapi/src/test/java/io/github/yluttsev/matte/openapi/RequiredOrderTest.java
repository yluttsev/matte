package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RequiredOrderTest {

    private static final List<String> UNSORTED_REQUIRED = List.of("z", "a");

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @Test
    void testAddComponent_WhenYamlRequiredIsNotSorted_ShouldSucceedAndKeepOrder() throws IOException {
        assertAddComponent("additions-empty.yaml");
    }

    @Test
    void testAddComponent_WhenJsonRequiredIsNotSorted_ShouldSucceedAndKeepOrder() throws IOException {
        assertAddComponent("additions-empty.json");
    }

    @Test
    void testUpdateSchema_WhenYamlRequiredIsNotSorted_ShouldSucceedAndKeepOrder() throws IOException {
        assertUpdateRequired("required-order-update.yaml");
    }

    @Test
    void testUpdateSchema_WhenJsonRequiredIsNotSorted_ShouldSucceedAndKeepOrder() throws IOException {
        assertUpdateRequired("required-order-update.json");
    }

    private void assertAddComponent(String fixture) throws IOException {
        ImportResult imported = importFixture(fixture);
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.OBJECT))
                .properties(List.of(property("z"), property("a")))
                .required(new LinkedHashSet<>(UNSORTED_REQUIRED))
                .build();

        OpenApiEditResult result = editor.addComponent(
                imported,
                new AddSchemaComponent(new SchemaComponent(ElementId.random(), "Customer", schema))
        );

        assertInstanceOf(OpenApiEditResult.Applied.class, result);
        assertEquals(UNSORTED_REQUIRED, requiredOf(reimport(result, imported)));
        assertEquals(UNSORTED_REQUIRED, requiredOf(result.state().document().orElseThrow()));
    }

    private void assertUpdateRequired(String fixture) throws IOException {
        ImportResult imported = importFixture(fixture);
        SchemaObject schema = (SchemaObject) imported.document().orElseThrow()
                .getComponents().getSchemas().getFirst().getSchema();
        SchemaObject replacement = SchemaObject.builder(schema)
                .required(new LinkedHashSet<>(UNSORTED_REQUIRED))
                .build();

        OpenApiEditResult result = editor.updateSchema(imported, new UpdateSchemaObject(replacement));

        assertInstanceOf(OpenApiEditResult.Applied.class, result);
        assertEquals(UNSORTED_REQUIRED, requiredOf(reimport(result, imported)));
    }

    private SchemaProperty property(String name) {
        SchemaObject schema = SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.STRING)).build();
        return new SchemaProperty(ElementId.random(), name, schema);
    }

    private ApiDocument reimport(OpenApiEditResult result, ImportResult before) {
        ImportResult reimported = importer.importDocument(
                result.source().orElseThrow().content(),
                ImportOptions.withFormat(before.format().orElseThrow())
        );
        assertEquals(ImportStatus.IMPORTED, reimported.status(), reimported.diagnostics().toString());
        return reimported.document().orElseThrow();
    }

    private List<String> requiredOf(ApiDocument document) {
        SchemaObject schema = (SchemaObject) document.getComponents().getSchemas().getFirst().getSchema();
        return List.copyOf(schema.getRequired());
    }

    private ImportResult importFixture(String fixture) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(fixture), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }
}
