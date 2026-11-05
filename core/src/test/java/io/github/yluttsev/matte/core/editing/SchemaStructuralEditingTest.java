package io.github.yluttsev.matte.core.editing;

import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyRenamed;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndexBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaStructuralEditingTest {

    private final DocumentEditor editor = new DocumentEditor();

    @Test
    void testRenameProperty_WhenRequiredAndLocalReferencesTargetProperty_ShouldUpdateNameAndPointers() {
        ElementId propertyId = ElementId.random();
        SchemaProperty property = property("id", propertyId, SchemaObject.builder(ElementId.random())
                .reference(new SchemaReference("#/components/schemas/User/properties/id"))
                .build());
        SchemaObject user = SchemaObject.builder(ElementId.random())
                .properties(List.of(property))
                .required(Set.of("id"))
                .build();
        SchemaObject holder = SchemaObject.builder(ElementId.random())
                .properties(List.of(property("userId", ElementId.random(), SchemaObject.builder(ElementId.random())
                        .reference(new SchemaReference("#/components/schemas/User/properties/id"))
                        .build())))
                .build();
        ApiDocument original = document(component("User", user), component("Holder", holder));

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(
                original,
                new RenameSchemaProperty(user.getId(), propertyId, "identifier")
        ));

        SchemaObject renamed = schema(result.document(), "User");
        assertEquals(propertyId, renamed.getProperties().getFirst().getId());
        assertEquals("identifier", renamed.getProperties().getFirst().getName());
        assertEquals(Set.of("identifier"), renamed.getRequired());
        assertEquals("id", schema(original, "User").getProperties().getFirst().getName());
        assertEquals(Set.of("id"), schema(original, "User").getRequired());
        SchemaObject movedPropertySchema = (SchemaObject) renamed.getProperties().getFirst().getSchema();
        assertEquals("#/components/schemas/User/properties/identifier", movedPropertySchema.getReference().value());
        assertEquals(
                "#/components/schemas/User/properties/identifier",
                ((SchemaObject) schema(result.document(), "Holder").getProperties().getFirst().getSchema())
                        .getReference().value()
        );
        assertTrue(result.changeSet().changes().stream().anyMatch(SchemaPropertyRenamed.class::isInstance));
        assertEquals(2, result.changeSet().changes().stream().filter(ReferenceChanged.class::isInstance).count());
        assertEquals(
                List.of(
                        DocumentPath.parse("/components/schemas/User/properties/identifier/$ref"),
                        DocumentPath.parse("/components/schemas/Holder/properties/userId/$ref")
                ),
                new ReferenceIndexBuilder().build(DocumentIndex.build(result.document()))
                        .usages().stream().map(usage -> usage.sourcePath()).toList()
        );
    }

    @Test
    void testRenameProperty_WhenNameConflicts_ShouldRejectWithoutChangingDocument() {
        ElementId id = ElementId.random();
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .properties(List.of(
                        property("id", id, SchemaObject.builder(ElementId.random()).build()),
                        property("name", ElementId.random(), SchemaObject.builder(ElementId.random()).build())
                ))
                .build();
        ApiDocument original = document(component("User", schema));

        EditRejected result = assertInstanceOf(EditRejected.class, editor.apply(
                original,
                new RenameSchemaProperty(schema.getId(), id, "name")
        ));

        assertEquals(EditCode.NAME_CONFLICT, result.issues().getFirst().code());
        assertEquals(List.of("id", "name"), schema(original, "User").getProperties().stream()
                .map(SchemaProperty::getName).toList());
    }

    @Test
    void testRenameProperty_WhenPointerUsesEscapedNestedPath_ShouldRelocatePropertyAndDescendant() {
        SchemaProperty property = property(
                "old/name~",
                ElementId.random(),
                SchemaObject.builder(ElementId.random())
                        .properties(List.of(property("code", ElementId.random(),
                                SchemaObject.builder(ElementId.random()).build())))
                        .build()
        );
        SchemaObject user = SchemaObject.builder(ElementId.random()).properties(List.of(property)).build();
        SchemaObject holder = SchemaObject.builder(ElementId.random())
                .properties(List.of(property("code", ElementId.random(), SchemaObject.builder(ElementId.random())
                        .reference(new SchemaReference(
                                "#/components/schemas/User/properties/old~1name~0/properties/code"
                        ))
                        .build())))
                .build();
        ApiDocument original = document(component("User", user), component("Holder", holder));

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(
                original,
                new RenameSchemaProperty(user.getId(), property.getId(), "new/name~")
        ));

        SchemaObject updatedReference = (SchemaObject) schema(result.document(), "Holder")
                .getProperties().getFirst().getSchema();
        assertEquals(
                "#/components/schemas/User/properties/new~1name~0/properties/code",
                updatedReference.getReference().value()
        );
    }

    @Test
    void testDeleteProperty_WhenReferencedFromAnotherProperty_ShouldListUsageAndPreserveDocument() {
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .properties(List.of(
                        property("id", ElementId.random(), SchemaObject.builder(ElementId.random()).build()),
                        property("alias", ElementId.random(), SchemaObject.builder(ElementId.random())
                                .reference(new SchemaReference("#/components/schemas/User/properties/id"))
                                .build())
                ))
                .build();
        ApiDocument original = document(component("User", schema));
        String originalId = schema(original, "User").getProperties().getFirst().getName();

        EditRejected result = assertInstanceOf(EditRejected.class, editor.apply(
                original,
                new DeleteSchemaProperty(schema.getId(), schema.getProperties().getFirst().getId())
        ));

        assertEquals(EditCode.REFERENCED_TARGET, result.issues().getFirst().code());
        assertTrue(result.issues().getFirst().message().contains("/components/schemas/User/properties/alias/$ref"));
        assertEquals(originalId, schema(original, "User").getProperties().getFirst().getName());
        assertEquals(2, schema(original, "User").getProperties().size());
    }

    @Test
    void testDeleteProperty_WhenOnlySelfReferenced_ShouldDeleteTheWholePropertySubtree() {
        SchemaProperty property = property("self", ElementId.random(), SchemaObject.builder(ElementId.random())
                .reference(new SchemaReference("#/components/schemas/User/properties/self"))
                .build());
        SchemaObject schema = SchemaObject.builder(ElementId.random()).properties(List.of(property)).build();
        ApiDocument original = document(component("User", schema));

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(
                original,
                new DeleteSchemaProperty(schema.getId(), property.getId())
        ));

        assertTrue(schema(result.document(), "User").getProperties().isEmpty());
        assertEquals(1, schema(original, "User").getProperties().size());
    }

    @Test
    void testDeleteComponent_WhenReferencedFromAnotherComponent_ShouldListUsageAndPreserveDocument() {
        SchemaComponent user = component("User", SchemaObject.builder(ElementId.random()).build());
        SchemaComponent holder = component("Holder", SchemaObject.builder(ElementId.random())
                .properties(List.of(property("user", ElementId.random(), SchemaObject.builder(ElementId.random())
                        .reference(new SchemaReference("#/components/schemas/User"))
                        .build())))
                .build());
        ApiDocument original = document(user, holder);

        EditRejected result = assertInstanceOf(EditRejected.class, editor.apply(
                original,
                new DeleteSchemaComponent(user.getId())
        ));

        assertEquals(EditCode.REFERENCED_TARGET, result.issues().getFirst().code());
        assertTrue(result.issues().getFirst().message().contains("/components/schemas/Holder/properties/user/$ref"));
        assertEquals(2, original.getComponents().getSchemas().size());
    }

    @Test
    void testDeleteComponent_WhenReferencesStayInsideDeletedSubtree_ShouldDeleteComponent() {
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .properties(List.of(property("self", ElementId.random(), SchemaObject.builder(ElementId.random())
                        .reference(new SchemaReference("#/components/schemas/User"))
                        .build())))
                .build();
        ApiDocument original = document(component("User", schema));

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(
                original,
                new DeleteSchemaComponent(original.getComponents().getSchemas().getFirst().getId())
        ));

        assertTrue(result.document().getComponents().getSchemas().isEmpty());
        assertEquals(1, original.getComponents().getSchemas().size());
    }

    private ApiDocument document(SchemaComponent... components) {
        return ApiDocument.builder(ElementId.random())
                .components(Components.builder().schemas(List.of(components)).build())
                .build();
    }

    private SchemaComponent component(String name, SchemaObject schema) {
        return new SchemaComponent(ElementId.random(), name, schema);
    }

    private SchemaProperty property(String name, ElementId id, SchemaObject schema) {
        return new SchemaProperty(id, name, schema);
    }

    private SchemaObject schema(ApiDocument document, String name) {
        return (SchemaObject) document.getComponents().getSchemas().stream()
                .filter(component -> component.getName().equals(name))
                .findFirst().orElseThrow().getSchema();
    }
}
