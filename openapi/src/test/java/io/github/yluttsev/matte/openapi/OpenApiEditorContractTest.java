package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.List;
import io.github.yluttsev.matte.core.editing.DocumentEditor;
import io.github.yluttsev.matte.core.editing.EditCode;
import io.github.yluttsev.matte.core.editing.EditIssue;
import io.github.yluttsev.matte.core.editing.EditRejected;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static io.github.yluttsev.matte.openapi.testing.EditAssertions.assertRefused;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OpenApiEditorContractTest {

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiEditor editor = new OpenApiEditor();

    @ParameterizedTest
    @EnumSource(SourceEditCode.class)
    void testApply_WhenSourceEditIsUnsafe_ShouldReturnTypedAtomicRefusal(SourceEditCode code) throws IOException {
        ImportResult input = importFixture(fixture(code));
        EditCommand command = command(input, code);

        OpenApiEditResult.Refused refusal = assertRefused(input, () -> editor.apply(input, command));

        assertEquals(code, refusal.code());
        assertEquals(expectedPath(code), refusal.path());
    }

    @Test
    void testRename_WhenIncomingReferenceIsSharedThroughAlias_ShouldRefuseAtomically() throws IOException {
        ImportResult input = importFixture("edit-outcome-shared-reference.yaml");
        RenameSchemaComponent command = new RenameSchemaComponent(component(input, "Target").getId(), "New");

        OpenApiEditResult.Refused refusal = assertRefused(input, () -> editor.rename(input, command));

        assertEquals(SourceEditCode.SHARED_YAML_NODE, refusal.code());
        assertEquals("Target", component(input, "Target").getName());
    }

    @Test
    void testApply_WhenInjectedEditorRejects_ShouldUseItsIssuesAndSkipImporter() throws IOException {
        ImportResult input = importFixture("additions-empty.yaml");
        DocumentEditor semantic = mock(DocumentEditor.class);
        OpenApiImporter injectedImporter = mock(OpenApiImporter.class);
        EditRejected rejection = new EditRejected(List.of(new EditIssue(EditCode.INVALID_NAME, "Test rejection")));
        when(semantic.apply(any(), any())).thenReturn(rejection);

        OpenApiEditResult result = new OpenApiEditor(semantic, injectedImporter).apply(input, addition());

        assertSame(rejection, assertInstanceOf(OpenApiEditResult.Rejected.class, result).edit());
        assertSame(input, result.state());
        assertTrue(result.source().isEmpty());
        verify(semantic).apply(any(), any());
        verifyNoInteractions(injectedImporter);
    }

    @Test
    void testApply_WhenDependenciesAreInjected_ShouldUseBothAndPreserveIdentifiers() throws IOException {
        ImportResult input = importFixture("additions-empty.yaml");
        DocumentEditor semantic = spy(new DocumentEditor());
        OpenApiImporter injectedImporter = spy(new OpenApiImporter());
        AddSchemaComponent command = addition();

        OpenApiEditResult result = new OpenApiEditor(semantic, injectedImporter).apply(input, command);

        OpenApiEditResult.Applied applied = assertInstanceOf(OpenApiEditResult.Applied.class, result);
        verify(semantic).apply(any(), any());
        verify(injectedImporter).importDocument(any(), any());
        assertEquals(command.component().getId(), result.state().document().orElseThrow()
                .getComponents().getSchemas().getFirst().getId());
        assertArrayEquals(applied.updatedSource().content(), result.state().source().content());
        assertTrue(input.document().orElseThrow().getComponents().getSchemas().isEmpty());
    }

    @Test
    void testApply_WhenReimportDisagreesWithSemanticResult_ShouldThrowInternalError() throws IOException {
        ImportResult input = importFixture("additions-empty.yaml");
        byte[] original = input.source().content();
        OpenApiImporter injectedImporter = spy(new OpenApiImporter());
        doReturn(input).when(injectedImporter).importDocument(any(), any());

        assertThrows(IllegalStateException.class,
                () -> new OpenApiEditor(new DocumentEditor(), injectedImporter).apply(input, addition()));

        assertArrayEquals(original, input.source().content());
        assertTrue(input.document().orElseThrow().getComponents().getSchemas().isEmpty());
    }

    @Test
    void testApply_WhenCallerInputIsInvalid_ShouldThrowIllegalArgumentException() throws IOException {
        ImportResult invalid = importer.importDocument(new byte[0], ImportOptions.autoDetect());
        ImportResult input = importFixture("additions-empty.yaml");

        assertThrows(IllegalArgumentException.class, () -> editor.apply(invalid, addition()));
        assertThrows(IllegalArgumentException.class, () -> editor.apply(null, addition()));
        assertThrows(IllegalArgumentException.class, () -> editor.apply(input, null));
        assertThrows(IllegalArgumentException.class, () -> new OpenApiEditor(null, importer));
        assertThrows(IllegalArgumentException.class, () -> new OpenApiEditor(new DocumentEditor(), null));
    }

    private String fixture(SourceEditCode code) {
        return switch (code) {
            case SHARED_YAML_NODE -> "yaml-alias-schema.yaml";
            case AMBIGUOUS_COMMENT -> "schema-field-editing-inline-comment.yaml";
            case MERGE_KEY -> "source-ranges-merge.yaml";
            case AMBIGUOUS_LAYOUT -> "edit-outcome-layout.yaml";
            case NESTED_REPLACEMENT -> "schema-field-editing-3.1.yaml";
            case SCHEMA_ID_SCOPE -> "schema-id-delete.yaml";
            case UNRECOGNIZED_REFERENCE -> "schema-reference-unknown-only.yaml";
            case UNSUPPORTED_IN_VERSION -> "schema-field-editing-3.0-const.yaml";
        };
    }

    private EditCommand command(ImportResult input, SourceEditCode code) {
        return switch (code) {
            case SHARED_YAML_NODE -> updateTitle(input, "Shared");
            case AMBIGUOUS_COMMENT -> new UpdateSchemaObject(
                    SchemaObject.builder(schema(input, "Customer")).minLength(null).build());
            case MERGE_KEY -> new AddSchemaProperty(schema(input, "Old").getId(),
                    new SchemaProperty(ElementId.random(), "new", new SchemaObject(ElementId.random())));
            case AMBIGUOUS_LAYOUT -> addition();
            case NESTED_REPLACEMENT -> new UpdateSchemaObject(
                    SchemaObject.builder(schema(input, "Customer")).properties(List.of()).build());
            case SCHEMA_ID_SCOPE -> new RenameSchemaComponent(component(input, "Canonical").getId(), "New");
            case UNRECOGNIZED_REFERENCE -> new RenameSchemaComponent(component(input, "Old").getId(), "New");
            case UNSUPPORTED_IN_VERSION -> new UpdateSchemaObject(
                    SchemaObject.builder(schema(input, "Customer")).constValue(new NullValue()).build());
        };
    }

    private DocumentPath expectedPath(SourceEditCode code) {
        return switch (code) {
            case SHARED_YAML_NODE -> DocumentPath.schemaComponent("Shared").child("title");
            case AMBIGUOUS_COMMENT, UNSUPPORTED_IN_VERSION -> DocumentPath.schemaComponent("Customer");
            case MERGE_KEY -> DocumentPath.schemaComponent("Old").child("properties").child("new");
            case AMBIGUOUS_LAYOUT -> DocumentPath.schemaComponent("New");
            case NESTED_REPLACEMENT -> DocumentPath.schemaComponent("Customer").child("properties");
            case SCHEMA_ID_SCOPE -> DocumentPath.schemaComponent("Canonical").child("$id");
            case UNRECOGNIZED_REFERENCE -> DocumentPath.schemaComponent("Holder")
                    .child("custom-keyword").child("$ref");
        };
    }

    private UpdateSchemaObject updateTitle(ImportResult input, String name) {
        return new UpdateSchemaObject(SchemaObject.builder(schema(input, name)).title("Changed").build());
    }

    private AddSchemaComponent addition() {
        return new AddSchemaComponent(new SchemaComponent(
                ElementId.random(), "New", new SchemaObject(ElementId.random())));
    }

    private SchemaObject schema(ImportResult input, String name) {
        return (SchemaObject) component(input, name).getSchema();
    }

    private SchemaComponent component(ImportResult input, String name) {
        return input.document().orElseThrow().getComponents().getSchemas().stream()
                .filter(component -> component.getName().equals(name)).findFirst().orElseThrow();
    }

    private ImportResult importFixture(String name) throws IOException {
        ImportResult input = importer.importDocument(TestFixtures.bytes(name), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, input.status(), input.diagnostics().toString());
        return input;
    }
}
