package io.github.yluttsev.matte.core.construction;

import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.yluttsev.matte.core.construction.ModelFixture.referenceComponent;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentStructureValidatorTest {

    @Test
    void testValidate_WhenDocumentAndCopyAreStructurallyValid_ShouldAcceptBoth() {
        ApiDocument document = ModelFixture.document();

        assertDoesNotThrow(() -> DocumentStructureValidator.validate(document));
        assertDoesNotThrow(() -> DocumentStructureValidator.validate(document.copy()));
    }

    @Test
    void testBuild_WhenPathsAreDuplicated_ShouldRejectWithBothLocations() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApiDocument.builder(ElementId.random()).paths(List.of(
                        new PathItem(ElementId.random(), "/users"),
                        new PathItem(ElementId.random(), "/users"))).build());

        assertEquals("Duplicate name '/users' at $.paths[1]; first at $.paths[0]", exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("duplicateComponents")
    void testBuild_WhenComponentNamesAreDuplicated_ShouldRejectWithBothLocations(
            String section,
            Components components
    ) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApiDocument.builder(ElementId.random()).components(components).build());

        assertEquals("Duplicate name 'Same' at $.components." + section
                + "[1]; first at $.components." + section + "[0]", exception.getMessage());
    }

    @Test
    void testBuild_WhenSchemaPropertyNamesAreDuplicated_ShouldRejectWithBothLocations() {
        SchemaObject schema = SchemaObject.builder(ElementId.random()).properties(List.of(
                new SchemaProperty(ElementId.random(), "name", new BooleanSchema(ElementId.random(), true)),
                new SchemaProperty(ElementId.random(), "name", new BooleanSchema(ElementId.random(), false))))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> document(schema));

        assertEquals("Duplicate name 'name' at $.components.schemas[0].schema.properties[1]; first at "
                + "$.components.schemas[0].schema.properties[0]", exception.getMessage());
    }

    @Test
    void testBuild_WhenDifferentInstancesHaveSameId_ShouldRejectDuplicateId() {
        ElementId id = ElementId.random();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApiDocument.builder(ElementId.random()).paths(List.of(
                        new PathItem(id, "/users"), new PathItem(id, "/customers"))).build());

        assertEquals("Duplicate element ID " + id.value() + " at $.paths[1]; first at $.paths[0]", exception.getMessage());
    }

    @Test
    void testBuild_WhenDifferentElementTypesHaveSameId_ShouldRejectDuplicateId() {
        ElementId id = ElementId.random();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApiDocument.builder(id).paths(List.of(new PathItem(id, "/users"))).build());

        assertEquals("Duplicate element ID " + id.value() + " at $.paths[0]; first at $", exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("schemasWithDuplicateNestedId")
    void testBuild_WhenNestedSchemaHasDuplicateId_ShouldRejectWithNestedLocation(String field, SchemaObject schema) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> document(schema));

        assertTrue(exception.getMessage().startsWith("Duplicate element ID "));
        assertTrue(exception.getMessage().contains("at $.components.schemas[0].schema." + field));
        assertTrue(exception.getMessage().endsWith("first at $.components.schemas[0].schema"));
    }

    @Test
    void testBuild_WhenNamesMatchInDifferentNamespaces_ShouldAllowConstruction() {
        SchemaObject first = schemaWithProperty("name");
        SchemaObject second = schemaWithProperty("name");
        Components components = Components.builder()
                .schemas(List.of(new SchemaComponent(ElementId.random(), "Same", first),
                        new SchemaComponent(ElementId.random(), "Other", second)))
                .parameters(List.of(referenceComponent("Same")))
                .responses(List.of(referenceComponent("Same")))
                .build();

        assertDoesNotThrow(() -> ApiDocument.builder(ElementId.random()).components(components).build());
    }

    @Test
    void testBuild_WhenReferencesAreUnresolvedOrExternal_ShouldAllowConstruction() {
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .reference(new SchemaReference("#/components/schemas/Missing"))
                .build();
        Components components = Components.builder()
                .schemas(List.of(new SchemaComponent(ElementId.random(), "User", schema)))
                .parameters(List.of(referenceComponent("MissingParameter")))
                .build();

        assertDoesNotThrow(() -> ApiDocument.builder(ElementId.random()).components(components).build());
        assertDoesNotThrow(() -> document(SchemaObject.builder(ElementId.random())
                .reference(new SchemaReference("https://example.test/schemas.yaml#/User"))
                .build()));
    }

    @Test
    void testBuild_WhenValidationFails_ShouldPreserveInputsAndAllowBuilderReuse() {
        List<PathItem> paths = new ArrayList<>(List.of(
                new PathItem(ElementId.random(), "/users"), new PathItem(ElementId.random(), "/users")));
        ApiDocument.Builder builder = ApiDocument.builder(ElementId.random()).paths(paths);

        assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals(2, paths.size());
        assertEquals("/users", paths.get(1).getPath());

        ApiDocument document = assertDoesNotThrow(() -> builder.paths(List.of(paths.getFirst())).build());
        assertEquals(1, document.getPaths().size());
    }

    @Test
    void testPaths_WhenInputContainsNull_ShouldRejectNullElement() {
        List<PathItem> paths = new ArrayList<>();
        paths.add(null);

        assertThrows(NullPointerException.class, () -> ApiDocument.builder(ElementId.random()).paths(paths));
    }

    private static Stream<Arguments> duplicateComponents() {
        return Stream.of(
                Arguments.of("schemas", Components.builder().schemas(List.of(
                        new SchemaComponent(ElementId.random(), "Same", new BooleanSchema(ElementId.random(), true)),
                        new SchemaComponent(ElementId.random(), "Same", new BooleanSchema(ElementId.random(), false))))
                        .build()),
                Arguments.of("parameters", Components.builder().parameters(
                        List.of(referenceComponent("Same"), referenceComponent("Same"))).build()),
                Arguments.of("requestBodies", Components.builder().requestBodies(
                        List.of(referenceComponent("Same"), referenceComponent("Same"))).build()),
                Arguments.of("responses", Components.builder().responses(
                        List.of(referenceComponent("Same"), referenceComponent("Same"))).build()),
                Arguments.of("headers", Components.builder().headers(
                        List.of(referenceComponent("Same"), referenceComponent("Same"))).build()),
                Arguments.of("securitySchemes", Components.builder().securitySchemes(
                        List.of(referenceComponent("Same"), referenceComponent("Same"))).build()),
                Arguments.of("examples", Components.builder().examples(
                        List.of(referenceComponent("Same"), referenceComponent("Same"))).build()));
    }

    private static Stream<Arguments> schemasWithDuplicateNestedId() {
        ElementId id = ElementId.random();
        return Stream.of(
                Arguments.of("items", SchemaObject.builder(id).items(new BooleanSchema(id, true)).build()),
                Arguments.of("additionalProperties", SchemaObject.builder(id)
                        .additionalProperties(new BooleanSchema(id, true)).build()),
                Arguments.of("allOf[0]", SchemaObject.builder(id).allOf(List.of(new BooleanSchema(id, true))).build()),
                Arguments.of("oneOf[0]", SchemaObject.builder(id).oneOf(List.of(new BooleanSchema(id, true))).build()),
                Arguments.of("anyOf[0]", SchemaObject.builder(id).anyOf(List.of(new BooleanSchema(id, true))).build()),
                Arguments.of("not", SchemaObject.builder(id).not(new BooleanSchema(id, true)).build()));
    }

    private static SchemaObject schemaWithProperty(String name) {
        return SchemaObject.builder(ElementId.random())
                .properties(List.of(new SchemaProperty(ElementId.random(), name,
                        new BooleanSchema(ElementId.random(), true))))
                .build();
    }

    private static ApiDocument document(SchemaObject schema) {
        return ApiDocument.builder(ElementId.random())
                .components(Components.builder().schemas(List.of(
                        new SchemaComponent(ElementId.random(), "User", schema))).build())
                .build();
    }
}
