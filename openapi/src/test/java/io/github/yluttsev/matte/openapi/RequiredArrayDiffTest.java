package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequiredArrayDiffTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testUpdateSchema_WhenYamlBlockRequiredNameIsRemoved_ShouldRemoveOnlyThatLine() throws IOException {
        assertUpdate("required-order-diff.yaml", "required-order-diff-removed.yaml", List.of("a", "c"));
    }

    @Test
    void testUpdateSchema_WhenYamlBlockRequiredNameIsAdded_ShouldAppendOnlyThatLine() throws IOException {
        assertUpdate("required-order-diff.yaml", "required-order-diff-added.yaml", List.of("a", "b", "c", "d"));
    }

    @Test
    void testUpdateSchema_WhenYamlBlockRequiredNamesAreReplaced_ShouldChangeOnlyDifferentLines()
            throws IOException {
        assertUpdate("required-order-diff.yaml", "required-order-diff-swapped.yaml", List.of("b", "c", "d"));
    }

    @Test
    void testUpdateSchema_WhenYamlFlowRequiredNameIsRemoved_ShouldKeepOtherItemsUntouched() throws IOException {
        assertUpdate("required-order-diff-flow.yaml", "required-order-diff-flow-removed.yaml", List.of("a", "c"));
    }

    @Test
    void testUpdateSchema_WhenYamlFlowRequiredNameIsAdded_ShouldAppendWithExistingSeparator() throws IOException {
        assertUpdate(
                "required-order-diff-flow.yaml",
                "required-order-diff-flow-added.yaml",
                List.of("a", "b", "c", "d")
        );
    }

    @Test
    void testUpdateSchema_WhenJsonRequiredNameIsRemoved_ShouldKeepOtherItemsUntouched() throws IOException {
        assertUpdate("required-order-diff.json", "required-order-diff-removed.json", List.of("a", "c"));
    }

    @Test
    void testUpdateSchema_WhenJsonRequiredNameIsAdded_ShouldAppendWithExistingSeparator() throws IOException {
        assertUpdate("required-order-diff.json", "required-order-diff-added.json", List.of("a", "b", "c", "d"));
    }

    @Test
    void testUpdateSchema_WhenRequiredIsOnlyReordered_ShouldKeepSourceUntouched() throws IOException {
        for (String fixture : List.of(
                "required-order-diff.yaml",
                "required-order-diff-flow.yaml",
                "required-order-diff.json"
        )) {
            ImportResult imported = importFixture(fixture);

            OpenApiEditResult result = update(imported, List.of("c", "b", "a"));

            assertInstanceOf(EditSucceeded.class, result.edit());
            assertSame(imported.source(), result.source().orElseThrow());
        }
    }

    @Test
    void testUpdateSchema_WhenRemovedYamlRequiredItemHasComment_ShouldRefuseWithoutChangingSource()
            throws IOException {
        ImportResult imported = importFixture("required-order-comment.yaml");
        byte[] original = imported.source().content();

        assertThrows(IllegalArgumentException.class, () -> update(imported, List.of("a", "c")));

        assertArrayEquals(original, imported.source().content());
    }

    @Test
    void testUpdateSchema_WhenYamlRequiredIsSharedByAlias_ShouldRefuseWithoutChangingSource() throws IOException {
        ImportResult imported = importFixture("required-order-alias.yaml");
        byte[] original = imported.source().content();

        assertThrows(IllegalArgumentException.class, () -> update(imported, List.of("a")));

        assertArrayEquals(original, imported.source().content());
    }

    @Test
    void testRenameProperty_WhenYamlBlockNameIsAtTheEnd_ShouldKeepPositionInRequired() throws IOException {
        assertRename("required-order-rename.yaml", "required-order-rename-renamed.yaml", List.of("m", "a"));
    }

    @Test
    void testRenameProperty_WhenYamlFlowNameIsAtTheEnd_ShouldKeepPositionInRequired() throws IOException {
        assertRename(
                "required-order-rename-flow.yaml",
                "required-order-rename-flow-renamed.yaml",
                List.of("m", "a")
        );
    }

    @Test
    void testRenameProperty_WhenJsonNameIsAtTheStart_ShouldKeepPositionInRequired() throws IOException {
        assertRename(
                "required-order-rename-start.json",
                "required-order-rename-start-renamed.json",
                List.of("a", "m")
        );
    }

    @Test
    void testImport_WhenRequiredIsNotSorted_ShouldKeepSourceOrder() throws IOException {
        for (String fixture : List.of(
                "required-order-import.yaml",
                "required-order-import-flow.yaml",
                "required-order-import.json"
        )) {
            assertEquals(
                    List.of("z", "a", "m"),
                    List.copyOf(customer(importFixture(fixture).document().orElseThrow()).getRequired())
            );
        }
    }

    private void assertUpdate(String sourceFixture, String expectedFixture, List<String> required)
            throws IOException {
        ImportResult imported = importFixture(sourceFixture);

        OpenApiEditResult result = update(imported, required);

        assertInstanceOf(EditSucceeded.class, result.edit());
        assertArrayEquals(TestFixtures.bytes(expectedFixture), result.source().orElseThrow().content());
        assertEquals(required, List.copyOf(customer(result.state().document().orElseThrow()).getRequired()));
        assertEquals(required, List.copyOf(customer(reimport(result, imported)).getRequired()));
    }

    private void assertRename(String sourceFixture, String expectedFixture, List<String> required)
            throws IOException {
        ImportResult imported = importFixture(sourceFixture);
        SchemaObject customer = customer(imported.document().orElseThrow());
        SchemaProperty renamed = customer.getProperties().stream()
                .filter(property -> property.getName().equals("b"))
                .findFirst()
                .orElseThrow();

        OpenApiEditResult result = editor.renameProperty(
                imported,
                new RenameSchemaProperty(customer.getId(), renamed.getId(), "a")
        );

        assertInstanceOf(EditSucceeded.class, result.edit());
        assertArrayEquals(TestFixtures.bytes(expectedFixture), result.source().orElseThrow().content());
        assertEquals(required, List.copyOf(customer(result.state().document().orElseThrow()).getRequired()));
        assertEquals(required, List.copyOf(customer(reimport(result, imported)).getRequired()));
    }

    private OpenApiEditResult update(ImportResult imported, List<String> required) {
        SchemaObject customer = customer(imported.document().orElseThrow());
        SchemaObject replacement = SchemaObject.builder(customer)
                .required(new LinkedHashSet<>(required))
                .build();
        return editor.updateSchema(imported, new UpdateSchemaObject(replacement));
    }

    private ApiDocument reimport(OpenApiEditResult result, ImportResult before) {
        ImportResult reimported = importer.importDocument(
                result.source().orElseThrow().content(),
                ImportOptions.withFormat(before.format().orElseThrow())
        );
        assertEquals(ImportStatus.IMPORTED, reimported.status(), reimported.diagnostics().toString());
        return reimported.document().orElseThrow();
    }

    private SchemaObject customer(ApiDocument document) {
        return (SchemaObject) document.getComponents().getSchemas().getFirst().getSchema();
    }

    private ImportResult importFixture(String fixture) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(fixture), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }
}
