package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredPropertyRenameTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testRenameProperty_WhenYamlBlockRequiredContainsNewName_ShouldRemoveOldEntryAndKeepOrder() throws IOException {
        assertRename("required-rename-block.yaml", "required-rename-block-renamed.yaml", List.of("newName", "later"));
    }

    @Test
    void testRenameProperty_WhenYamlFlowRequiredContainsNewName_ShouldKeepOtherEntriesInOrder() throws IOException {
        assertRename("required-rename-flow.yaml", "required-rename-flow-renamed.yaml", List.of("later", "newName"));
    }

    @Test
    void testRenameProperty_WhenJsonRequiredContainsNewName_ShouldKeepOtherEntriesInOrder() throws IOException {
        assertRename("required-rename.json", "required-rename-renamed.json", List.of("newName", "later"));
    }

    @Test
    void testRenameProperty_WhenRequiredContainsRepeatedOldName_ShouldRemoveEveryOldEntry() throws IOException {
        assertRename(
                "required-rename-repeated.json",
                "required-rename-repeated-renamed.json",
                List.of("newName", "later")
        );
    }

    private void assertRename(
            String sourceFixture,
            String expectedFixture,
            List<String> expectedRequired
    )
            throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(sourceFixture), ImportOptions.autoDetect());
        SchemaObject customer = schema(imported.document().orElseThrow());
        SchemaProperty property = customer.getProperties().stream()
                .filter(candidate -> candidate.getName().equals("oldName"))
                .findFirst()
                .orElseThrow();

        OpenApiEditResult result = editor.renameProperty(
                imported,
                new RenameSchemaProperty(customer.getId(), property.getId(), "newName")
        );

        assertInstanceOf(EditSucceeded.class, result.edit());
        byte[] source = result.source().orElseThrow().content();
        assertArrayEquals(TestFixtures.bytes(expectedFixture), source);
        assertEquals(Set.copyOf(expectedRequired), schema(result.state().document().orElseThrow()).getRequired());

        ImportResult reimported = importer.importDocument(
                source,
                ImportOptions.withFormat(imported.format().orElseThrow())
        );
        assertEquals(ImportStatus.IMPORTED, reimported.status(), reimported.diagnostics().toString());
        SchemaObject checked = schema(reimported.document().orElseThrow());
        assertEquals(Set.copyOf(expectedRequired), checked.getRequired());
        assertTrue(checked.getProperties().stream().anyMatch(candidate -> candidate.getName().equals("newName")));
        assertTrue(checked.getProperties().stream().noneMatch(candidate -> candidate.getName().equals("oldName")));
    }

    private SchemaObject schema(ApiDocument document) {
        return (SchemaObject) document.getComponents().getSchemas().stream()
                .filter(candidate -> candidate.getName().equals("Customer"))
                .findFirst()
                .orElseThrow()
                .getSchema();
    }
}
