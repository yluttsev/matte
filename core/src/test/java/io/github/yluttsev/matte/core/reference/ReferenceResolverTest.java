package io.github.yluttsev.matte.core.reference;

import java.util.List;
import java.util.stream.Stream;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceResolverTest {

    private final ReferenceResolver resolver = new ReferenceResolver();

    @Test
    void testResolve_WhenLocalSchemaExists_ShouldReturnTypedTargetAndPath() {
        DocumentIndex index = ReferenceFixture.index();
        ReferenceResolution<Schema> result = resolver.resolve(index, new SchemaReference("#/components/schemas/User"));

        ResolvedReference<?> resolved = assertInstanceOf(ResolvedReference.class, result);
        assertInstanceOf(SchemaObject.class, resolved.value());
        assertSame(
                index.findByPath(DocumentPath.parse("/components/schemas/User")).orElseThrow().value(),
                resolved.value()
        );
        assertEquals(DocumentPath.parse("/components/schemas/User"), resolved.targetPath());
        assertEquals(List.of(resolved.targetPath()), resolved.traversedPaths());
        assertThrows(UnsupportedOperationException.class, resolved.traversedPaths()::clear);
    }

    @Test
    void testResolve_WhenPointerTargetsNestedBooleanSchema_ShouldReturnNestedTarget() {
        ResolvedReference<?> result = assertInstanceOf(ResolvedReference.class, resolver.resolve(
                ReferenceFixture.index(), new SchemaReference("#/components/schemas/User/properties/name")));

        assertInstanceOf(BooleanSchema.class, result.value());
    }

    @Test
    void testResolve_WhenComponentNameContainsReservedCharacters_ShouldResolveEncodedPointer() {
        String name = "a/b~ c%+#имя";
        DocumentPath path = DocumentPath.root().child("components").child("schemas").child(name);
        DocumentIndex index = DocumentIndex.build(ApiDocument.builder(ElementId.random()).components(
                Components.builder().schemas(List.of(new SchemaComponent(ElementId.random(), name,
                        new BooleanSchema(ElementId.random(), true)))).build()).build());

        ResolvedReference<?> result = assertInstanceOf(ResolvedReference.class,
                resolver.resolve(index, new SchemaReference(path.toFragment())));

        assertEquals(path, result.targetPath());
    }

    @Test
    void testResolve_WhenParameterComponentIsAlias_ShouldFollowAliasAndRecordChain() {
        ResolvedReference<?> result = assertInstanceOf(ResolvedReference.class,
                resolver.resolve(ReferenceFixture.index(), "#/components/parameters/Alias", Parameter.class));

        assertInstanceOf(Parameter.class, result.value());
        assertEquals(List.of(DocumentPath.parse("/components/parameters/Alias"),
                DocumentPath.parse("/components/parameters/Limit")), result.traversedPaths());
        assertEquals(DocumentPath.parse("/components/parameters/Limit"), result.targetPath());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "#"})
    void testResolve_WhenReferenceTargetsDocumentRoot_ShouldReturnDocument(String reference) {
        DocumentIndex index = ReferenceFixture.index();
        ResolvedReference<?> result = assertInstanceOf(ResolvedReference.class,
                resolver.resolve(index, reference, ApiDocument.class));

        assertEquals(DocumentPath.root(), result.targetPath());
    }

    @Test
    void testResolve_WhenSchemaReferencesItself_ShouldReturnSchemaWithoutExpandingIt() {
        ResolvedReference<?> result = assertInstanceOf(ResolvedReference.class,
                resolver.resolve(ReferenceFixture.index(), new SchemaReference("#/components/schemas/Self")));

        assertEquals(1, result.traversedPaths().size());
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("unresolvedReferences")
    void testResolve_WhenReferenceCannotResolve_ShouldReturnSpecificFailure(String reference, ReferenceFailure reason) {
        UnresolvedReference<?> result = assertInstanceOf(UnresolvedReference.class,
                resolver.resolve(ReferenceFixture.index(), reference, Schema.class));

        assertEquals(reason, result.reason());
        assertThrows(UnsupportedOperationException.class, result.traversedPaths()::clear);
    }

    @Test
    void testResolve_WhenAliasCycleExists_ShouldRejectCycleWithoutInfiniteRecursion() {
        UnresolvedReference<?> result = assertInstanceOf(UnresolvedReference.class,
                resolver.resolve(ReferenceFixture.index(), "#/components/parameters/CycleA", Parameter.class));

        assertEquals(ReferenceFailure.CYCLIC_REFERENCE, result.reason());
        assertEquals(List.of(DocumentPath.parse("/components/parameters/CycleA"),
                DocumentPath.parse("/components/parameters/CycleB")), result.traversedPaths());
    }

    @Test
    void testResolve_WhenAliasTargetIsMissingOrExternal_ShouldPreserveTraversedAlias() {
        UnresolvedReference<?> missing = assertInstanceOf(UnresolvedReference.class,
                resolver.resolve(ReferenceFixture.index(), "#/components/parameters/BrokenAlias", Parameter.class));
        UnresolvedReference<?> external = assertInstanceOf(UnresolvedReference.class,
                resolver.resolve(ReferenceFixture.index(), "#/components/parameters/ExternalAlias", Parameter.class));

        assertEquals(ReferenceFailure.TARGET_NOT_FOUND, missing.reason());
        assertEquals(2, missing.traversedPaths().size());
        assertEquals(ReferenceFailure.EXTERNAL_REFERENCE, external.reason());
        assertEquals(List.of(DocumentPath.parse("/components/parameters/ExternalAlias")), external.traversedPaths());
    }

    private static Stream<Arguments> unresolvedReferences() {
        return Stream.of(
                Arguments.of("#/missing", ReferenceFailure.TARGET_NOT_FOUND),
                Arguments.of("#/components/parameters/Limit", ReferenceFailure.TYPE_MISMATCH),
                Arguments.of("#/components/parameters/Alias", ReferenceFailure.TYPE_MISMATCH),
                Arguments.of("#", ReferenceFailure.TYPE_MISMATCH),
                Arguments.of("#/a~2", ReferenceFailure.INVALID_REFERENCE),
                Arguments.of("#/%FF", ReferenceFailure.INVALID_REFERENCE),
                Arguments.of("#/%", ReferenceFailure.INVALID_REFERENCE),
                Arguments.of("#/%GG", ReferenceFailure.INVALID_REFERENCE),
                Arguments.of("#/a b", ReferenceFailure.INVALID_REFERENCE),
                Arguments.of("#/a#b", ReferenceFailure.INVALID_REFERENCE),
                Arguments.of("#User", ReferenceFailure.UNSUPPORTED_ANCHOR),
                Arguments.of("https://example.test/spec.yaml#/User", ReferenceFailure.EXTERNAL_REFERENCE),
                Arguments.of("other.yaml#/User", ReferenceFailure.EXTERNAL_REFERENCE),
                Arguments.of("User", ReferenceFailure.EXTERNAL_REFERENCE),
                Arguments.of("/spec.yaml#/User", ReferenceFailure.EXTERNAL_REFERENCE),
                Arguments.of("?version=2#/User", ReferenceFailure.EXTERNAL_REFERENCE));
    }
}
