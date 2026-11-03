package io.github.yluttsev.matte.core.editing;

import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.construction.ModelAssertions;
import io.github.yluttsev.matte.core.construction.ModelFixture;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentAdded;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyAdded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentEditorTest {

    private final DocumentEditor editor = new DocumentEditor();

    @Test
    void testApply_WhenAddingSchemaComponent_ShouldAppendIsolatedComponentAndDescribeChange() {
        ApiDocument original = ModelFixture.document();
        ApiDocument snapshot = original.copy();
        SchemaComponent component = new SchemaComponent(ElementId.random(), "Customer", ModelFixture.schema());
        AddSchemaComponent command = new AddSchemaComponent(component);

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(original, command));

        assertEquals(List.of("User", "Customer"), result.document().getComponents().getSchemas().stream()
                .map(SchemaComponent::getName).toList());
        SchemaComponent added = result.document().getComponents().getSchemas().getLast();
        assertEquals(component.getId(), added.getId());
        ModelAssertions.assertIndependentCopy(component, command.component());
        ModelAssertions.assertIndependentCopy(command.component(), added);
        ModelAssertions.assertIndependentCopy(snapshot, original);
        assertNotSame(original, result.document());
        assertEquals(1, result.changeSet().changes().size());
        SchemaComponentAdded change = assertInstanceOf(
                SchemaComponentAdded.class,
                result.changeSet().changes().getFirst()
        );
        assertEquals(component.getId(), change.componentId());
        assertEquals("Customer", change.name());
        ModelAssertions.assertIndependentCopy(added.getSchema(), change.schema());
        assertThrows(UnsupportedOperationException.class, result.changeSet().changes()::clear);
    }

    @Test
    void testApply_WhenAddingProperty_ShouldPreserveSchemaFieldsAndRequiredNames() {
        SchemaObject schema = ModelFixture.schema();
        ApiDocument original = document(schema);
        ApiDocument snapshot = original.copy();
        SchemaProperty property = property("email");
        AddSchemaProperty command = new AddSchemaProperty(schema.getId(), property);

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(original, command));

        SchemaObject edited = (SchemaObject) DocumentIndex.build(result.document())
                .findById(schema.getId()).orElseThrow().value();
        assertEquals(List.of("name", "email"), edited.getProperties().stream().map(SchemaProperty::getName).toList());
        assertEquals(schema.getRequired(), edited.getRequired());
        assertEquals(schema.getTitle(), edited.getTitle());
        assertEquals(schema.getItems().getId(), edited.getItems().getId());
        ModelAssertions.assertIndependentCopy(property, command.property());
        ModelAssertions.assertIndependentCopy(property, edited.getProperties().getLast());
        ModelAssertions.assertIndependentCopy(snapshot, original);
        SchemaPropertyAdded change = assertInstanceOf(
                SchemaPropertyAdded.class,
                result.changeSet().changes().getFirst()
        );
        assertEquals(schema.getId(), change.schemaId());
        assertEquals(property.getId(), change.propertyId());
        assertEquals("email", change.name());
        ModelAssertions.assertIndependentCopy(property.getSchema(), change.schema());
    }

    @Test
    void testApply_WhenTargetIsInlineSchema_ShouldFindItById() {
        SchemaObject inline = SchemaObject.builder(ElementId.random()).build();
        SchemaObject parent = SchemaObject.builder(ElementId.random()).items(inline).build();

        EditSucceeded result = assertInstanceOf(EditSucceeded.class,
                editor.apply(document(parent), new AddSchemaProperty(inline.getId(), property("value"))));

        SchemaObject edited = (SchemaObject) DocumentIndex.build(result.document())
                .findById(inline.getId()).orElseThrow().value();
        assertEquals("value", edited.getProperties().getFirst().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void testApply_WhenNewNameIsBlank_ShouldRejectWithoutChangingOriginal(String name) {
        ApiDocument original = document(SchemaObject.builder(ElementId.random()).build());
        ElementId componentId = original.getComponents().getSchemas().getFirst().getId();
        ElementId schemaId = original.getComponents().getSchemas().getFirst().getSchema().getId();
        SchemaObject nestedWithBlankProperty = SchemaObject.builder(ElementId.random())
                .properties(List.of(property(name)))
                .build();

        assertRejected(original, new AddSchemaComponent(new SchemaComponent(
                ElementId.random(), name, new BooleanSchema(ElementId.random(), true))), EditCode.INVALID_NAME);
        assertRejected(original, new AddSchemaComponent(new SchemaComponent(
                ElementId.random(), "Customer", nestedWithBlankProperty)), EditCode.INVALID_NAME);
        assertRejected(original, new AddSchemaProperty(schemaId, property(name)), EditCode.INVALID_NAME);
        assertRejected(
                original,
                new AddSchemaProperty(
                        schemaId,
                        new SchemaProperty(ElementId.random(), "address", nestedWithBlankProperty)
                ),
                EditCode.INVALID_NAME
        );
        assertRejected(original, new RenameSchemaComponent(componentId, name), EditCode.INVALID_NAME);
    }

    @Test
    void testApply_WhenComponentOrPropertyNameExists_ShouldRejectNameConflict() {
        SchemaObject schema = SchemaObject.builder(ElementId.random()).properties(List.of(property("name"))).build();
        ApiDocument original = document(schema);

        assertRejected(original, new AddSchemaComponent(new SchemaComponent(
                ElementId.random(), "User", new BooleanSchema(ElementId.random(), true))), EditCode.NAME_CONFLICT);
        assertRejected(original, new AddSchemaProperty(schema.getId(), property("name")), EditCode.NAME_CONFLICT);
    }

    @Test
    void testApply_WhenIncomingIdExistsAnywhereInDocument_ShouldRejectIdConflict() {
        SchemaObject schema = SchemaObject.builder(ElementId.random()).build();
        ApiDocument original = document(schema);

        assertRejected(original, new AddSchemaComponent(new SchemaComponent(
                ElementId.random(), "Other", new BooleanSchema(original.getId(), true))), EditCode.ID_CONFLICT);
        assertRejected(original, new AddSchemaProperty(schema.getId(), new SchemaProperty(
                original.getId(), "value", new BooleanSchema(ElementId.random(), true))), EditCode.ID_CONFLICT);
    }

    @Test
    void testApply_WhenIncomingSubtreeContainsDuplicateIds_ShouldRejectBeforePublication() {
        ElementId duplicate = ElementId.random();
        SchemaObject invalid = SchemaObject.builder(duplicate).items(new BooleanSchema(duplicate, true)).build();

        assertRejected(new ApiDocument(ElementId.random()), new AddSchemaComponent(new SchemaComponent(
                ElementId.random(), "User", invalid)), EditCode.ID_CONFLICT);
    }

    @Test
    void testApply_WhenResultFailsStructuralValidation_ShouldDiscardMutatedWorkingCopy() {
        SchemaObject invalid = SchemaObject.builder(ElementId.random())
                .properties(List.of(property("duplicate"), property("duplicate"))).build();
        ApiDocument original = new ApiDocument(ElementId.random());

        assertRejected(original, new AddSchemaComponent(new SchemaComponent(
                ElementId.random(), "User", invalid)), EditCode.INVALID_STRUCTURE);
        assertTrue(original.getComponents().getSchemas().isEmpty());

        EditSucceeded retry = assertInstanceOf(EditSucceeded.class, editor.apply(original, new AddSchemaComponent(
                new SchemaComponent(ElementId.random(), "User", new BooleanSchema(ElementId.random(), true)))));
        assertEquals(1, retry.document().getComponents().getSchemas().size());
    }

    @Test
    void testApply_WhenTargetIsMissingOrHasWrongType_ShouldReturnSpecificIssue() {
        ApiDocument original = document(SchemaObject.builder(ElementId.random()).build());
        SchemaComponent component = original.getComponents().getSchemas().getFirst();

        assertRejected(original, new AddSchemaProperty(ElementId.random(), property("x")), EditCode.TARGET_NOT_FOUND);
        assertRejected(original, new RenameSchemaComponent(ElementId.random(), "Customer"), EditCode.TARGET_NOT_FOUND);
        assertRejected(
                original,
                new AddSchemaProperty(component.getId(), property("x")),
                EditCode.TARGET_TYPE_MISMATCH
        );
        assertRejected(
                original,
                new RenameSchemaComponent(component.getSchema().getId(), "Customer"),
                EditCode.TARGET_TYPE_MISMATCH
        );
    }

    @Test
    void testApply_WhenTargetSchemaIsBoolean_ShouldRejectPropertyAddition() {
        BooleanSchema schema = new BooleanSchema(ElementId.random(), true);
        ApiDocument original = ApiDocument.builder(ElementId.random()).components(Components.builder()
                .schemas(List.of(new SchemaComponent(ElementId.random(), "User", schema))).build()).build();

        assertRejected(original, new AddSchemaProperty(schema.getId(), property("x")), EditCode.TARGET_TYPE_MISMATCH);
    }

    @Test
    void testApply_WhenDocumentIsIncomplete_ShouldAllowEditingWithoutOpenApiValidation() {
        SchemaObject schema = SchemaObject.builder(ElementId.random()).required(Set.of("later")).build();

        EditSucceeded result = assertInstanceOf(EditSucceeded.class,
                editor.apply(document(schema), new AddSchemaProperty(schema.getId(), property("later"))));

        assertEquals(Set.of("later"), ((SchemaObject) result.document().getComponents().getSchemas().getFirst()
                .getSchema()).getRequired());
    }

    @Test
    void testApply_WhenArgumentsAreNull_ShouldRejectProgrammingError() {
        assertThrows(
                NullPointerException.class,
                () -> editor.apply(null, new RenameSchemaComponent(ElementId.random(), "User"))
        );
        assertThrows(NullPointerException.class, () -> editor.apply(new ApiDocument(ElementId.random()), null));
    }

    private void assertRejected(ApiDocument original, EditCommand command, EditCode code) {
        ApiDocument snapshot = original.copy();

        EditRejected rejected = assertInstanceOf(EditRejected.class, editor.apply(original, command));

        assertEquals(code, rejected.issues().getFirst().code());
        assertThrows(UnsupportedOperationException.class, rejected.issues()::clear);
        ModelAssertions.assertIndependentCopy(snapshot, original);
    }

    private static SchemaProperty property(String name) {
        return new SchemaProperty(ElementId.random(), name, new BooleanSchema(ElementId.random(), true));
    }

    private static ApiDocument document(SchemaObject schema) {
        return ApiDocument.builder(ElementId.random()).components(Components.builder().schemas(List.of(
                new SchemaComponent(ElementId.random(), "User", schema))).build()).build();
    }
}
