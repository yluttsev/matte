package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelAssertions;
import io.github.yluttsev.matte.core.construction.ModelFixture;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.path.HttpMethod;
import io.github.yluttsev.matte.core.model.path.Operation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.security.OAuthFlow;
import io.github.yluttsev.matte.core.model.security.OAuthFlows;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelWalkerTest {

    @Test
    void testWalk_WhenModelsAreNested_ShouldVisitInPreorderWithStableListAddresses() {
        SchemaObject name = SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.STRING)).build();
        SchemaObject user = SchemaObject.builder(ElementId.random())
                .properties(List.of(new SchemaProperty(ElementId.random(), "name", name)))
                .build();
        ApiDocument document = ApiDocument.builder(ElementId.random())
                .components(Components.builder().schemas(List.of(
                        new SchemaComponent(ElementId.random(), "User", user))).build())
                .build();
        List<String> paths = new ArrayList<>();

        new ModelWalker().walk(document, (node, path) -> paths.add(path));

        assertEquals(List.of(
                "$",
                "$.components",
                "$.components.schemas[0]",
                "$.components.schemas[0].schema",
                "$.components.schemas[0].schema.properties[0]",
                "$.components.schemas[0].schema.properties[0].schema"), paths);
    }

    @Test
    void testWalk_WhenDocumentUsesAllHandlers_ShouldVisitEveryOwnedModelOnce() {
        ApiDocument document = ModelFixture.document();
        Set<CopyableModel<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        new ModelWalker().walk(document, (node, path) -> assertTrue(visited.add(node), path));

        assertEquals(ModelAssertions.modelNodes(document), visited);
        assertEquals(27, visited.stream().map(Object::getClass).distinct().count());
    }

    @Test
    void testWalk_WhenChildrenAreInMap_ShouldPreserveOrderAndUseKeysInAddresses() {
        Map<HttpMethod, Operation> operations = new LinkedHashMap<>();
        operations.put(HttpMethod.POST, Operation.builder(ElementId.random()).build());
        operations.put(HttpMethod.GET, Operation.builder(ElementId.random()).build());
        PathItem path = PathItem.builder(ElementId.random(), "/users").operations(operations).build();
        List<String> addresses = new ArrayList<>();

        new ModelWalker().walk(path, (node, address) -> addresses.add(address));

        assertEquals(List.of("$", "$.operations[POST]", "$.operations[GET]"), addresses);
    }

    @Test
    void testWalk_WhenWalkerIsReused_ShouldNotRetainOwnershipState() {
        ApiDocument document = ModelFixture.document();
        ModelWalker walker = new ModelWalker();
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();

        walker.walk(document, (node, path) -> first.add(path));
        walker.walk(document, (node, path) -> second.add(path));

        assertFalse(first.isEmpty());
        assertEquals(first, second);
    }

    @Test
    void testWalk_WhenVisitorThrows_ShouldPropagateFailureAndAllowReuse() {
        ApiDocument document = new ApiDocument(ElementId.random());
        ModelWalker walker = new ModelWalker();
        IllegalStateException failure = new IllegalStateException("Visitor failed");

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> walker.walk(document, (node, path) -> {
                    throw failure;
                })));

        List<String> paths = new ArrayList<>();
        walker.walk(document, (node, path) -> paths.add(path));
        assertEquals(List.of("$", "$.components"), paths);
    }

    @Test
    void testWalk_WhenModelTypeIsUnknown_ShouldRejectExplicitly() {
        CopyableModel<Object> unknown = Object::new;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ModelWalker().walk(unknown, (node, path) -> {}));

        assertEquals("Unsupported model type at $: " + unknown.getClass(), exception.getMessage());
    }

    @Test
    void testWalk_WhenModelWithoutIdHasMultipleOwners_ShouldRejectRepeatedOwnership() throws ReflectiveOperationException {
        OAuthFlows flows = OAuthFlows.builder()
                .implicit(OAuthFlow.builder().build())
                .password(OAuthFlow.builder().build())
                .build();
        setField(flows, "password", flows.getImplicit());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ModelWalker().walk(flows, (node, path) -> {}));

        assertEquals("Repeated model ownership at $.password; first at $.implicit", exception.getMessage());
    }

    @Test
    void testWalk_WhenOwnershipCycleExists_ShouldRejectCycle() throws ReflectiveOperationException {
        SchemaObject schema = SchemaObject.builder(ElementId.random()).build();
        setField(schema, "items", schema);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ModelWalker().walk(schema, (node, path) -> {}));

        assertEquals("Repeated model ownership at $.items; first at $", exception.getMessage());
    }

    @Test
    void testWalk_WhenSchemaHasReference_ShouldNotFollowReference() {
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .reference(new SchemaReference("#/components/schemas/Self"))
                .build();
        List<String> paths = new ArrayList<>();

        new ModelWalker().walk(schema, (node, path) -> paths.add(path));

        assertEquals(List.of("$"), paths);
    }

    @Test
    void testWalk_WhenRootOrVisitorIsNull_ShouldRejectNullArgument() {
        ModelWalker walker = new ModelWalker();

        assertThrows(NullPointerException.class, () -> walker.walk(null, (node, path) -> {}));
        assertThrows(NullPointerException.class, () -> walker.walk(new Components(), null));
    }

    private static void setField(Object model, String name, Object value) throws ReflectiveOperationException {
        Field field = model.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(model, value);
    }
}
