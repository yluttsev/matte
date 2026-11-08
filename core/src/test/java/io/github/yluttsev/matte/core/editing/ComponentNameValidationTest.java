package io.github.yluttsev.matte.core.editing;

import java.util.List;
import io.github.yluttsev.matte.core.construction.ModelAssertions;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ComponentNameValidationTest {

    private final DocumentEditor editor = new DocumentEditor();

    @ParameterizedTest
    @ValueSource(strings = {".", "-", "_", "0", "123", "a", "Z", "User.Name-123_"})
    void testAddSchemaComponent_WhenNameMatchesPattern_ShouldAddComponent(String name) {
        ApiDocument original = document();
        SchemaComponent component = component(name);

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(
                original, new AddSchemaComponent(component)
        ));

        assertEquals(name, result.document().getComponents().getSchemas().getFirst().getName());
        assertEquals(component.getId(), result.document().getComponents().getSchemas().getFirst().getId());
        assertEquals(1, result.changeSet().changes().size());
        assertEquals(0, original.getComponents().getSchemas().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {".", "-", "_", "0", "123", "a", "Z", "User.Name-123_"})
    void testRenameSchemaComponent_WhenNameMatchesPattern_ShouldRenameComponent(String name) {
        SchemaComponent component = component("Original");
        ApiDocument original = document(component);

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(
                original, new RenameSchemaComponent(component.getId(), name)
        ));

        assertEquals(name, result.document().getComponents().getSchemas().getFirst().getName());
        assertEquals(component.getId(), result.document().getComponents().getSchemas().getFirst().getId());
        assertEquals("Original", original.getComponents().getSchemas().getFirst().getName());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "User Name", " User", "User ", "/", "~", "#", "имя", "é", "+", "%", "a\nb", "a\t"})
    void testAddSchemaComponent_WhenNameDoesNotMatchPattern_ShouldRejectWithoutMutation(String name) {
        ApiDocument original = EditingFixture.document();
        ApiDocument snapshot = original.copy();
        SchemaComponent candidate = component(name);
        SchemaComponent candidateSnapshot = candidate.copy();

        EditRejected result = assertInstanceOf(EditRejected.class, editor.apply(
                original, new AddSchemaComponent(candidate)
        ));

        assertEquals(List.of(EditCode.INVALID_NAME), result.issues().stream().map(EditIssue::code).toList());
        ModelAssertions.assertIndependentCopy(snapshot, original);
        ModelAssertions.assertIndependentCopy(candidateSnapshot, candidate);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "User Name", " User", "User ", "/", "~", "#", "имя", "é", "+", "%", "a\nb", "a\t"})
    void testRenameSchemaComponent_WhenNameDoesNotMatchPattern_ShouldRejectWithoutMutation(String name) {
        ApiDocument original = EditingFixture.document();
        ApiDocument snapshot = original.copy();
        SchemaComponent component = original.getComponents().getSchemas().getFirst();

        EditRejected result = assertInstanceOf(EditRejected.class, editor.apply(
                original, new RenameSchemaComponent(component.getId(), name)
        ));

        assertEquals(List.of(EditCode.INVALID_NAME), result.issues().stream().map(EditIssue::code).toList());
        ModelAssertions.assertIndependentCopy(snapshot, original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Legacy/Name", "имя", "Bad Name"})
    void testRenameSchemaComponent_WhenInvalidNameIsUnchanged_ShouldRejectWithoutMutation(String name) {
        SchemaComponent component = component(name);
        ApiDocument original = document(component);
        ApiDocument snapshot = original.copy();

        EditRejected result = assertInstanceOf(EditRejected.class, editor.apply(
                original, new RenameSchemaComponent(component.getId(), name)
        ));

        assertEquals(EditCode.INVALID_NAME, result.issues().getFirst().code());
        ModelAssertions.assertIndependentCopy(snapshot, original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"User Name", "/", "~", "#", "имя"})
    void testApply_WhenPropertyNameDoesNotMatchComponentPattern_ShouldAllowPropertyEdits(String name) {
        SchemaProperty property = new SchemaProperty(ElementId.random(), name, schema());
        SchemaObject schema = SchemaObject.builder(ElementId.random()).properties(List.of(property)).build();
        SchemaComponent component = new SchemaComponent(ElementId.random(), "Valid", schema);
        ApiDocument original = document();

        EditSucceeded added = assertInstanceOf(EditSucceeded.class, editor.apply(
                original, new AddSchemaComponent(component)
        ));
        assertEquals(name, ((SchemaObject) added.document().getComponents().getSchemas().getFirst().getSchema())
                .getProperties().getFirst().getName());

        SchemaProperty another = new SchemaProperty(ElementId.random(), name + " more", schema());
        EditSucceeded propertyAdded = assertInstanceOf(EditSucceeded.class, editor.apply(
                added.document(), new AddSchemaProperty(schema.getId(), another)
        ));
        EditSucceeded renamed = assertInstanceOf(EditSucceeded.class, editor.apply(
                propertyAdded.document(), new RenameSchemaProperty(schema.getId(), property.getId(), name + " renamed")
        ));
        SchemaObject edited = (SchemaObject) renamed.document().getComponents().getSchemas().getFirst().getSchema();
        assertEquals(List.of(name + " renamed", name + " more"),
                edited.getProperties().stream().map(SchemaProperty::getName).toList());
    }

    private static SchemaObject schema() {
        return SchemaObject.builder(ElementId.random()).build();
    }

    private static SchemaComponent component(String name) {
        return new SchemaComponent(ElementId.random(), name, schema());
    }

    private static ApiDocument document(SchemaComponent... components) {
        return ApiDocument.builder(ElementId.random())
                .components(Components.builder().schemas(List.of(components)).build()).build();
    }
}
