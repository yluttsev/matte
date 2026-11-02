package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.construction.ModelFixture;
import io.github.yluttsev.matte.core.construction.ModelAssertions;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceIndexTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("referenceSources")
    void testBuild_WhenReferenceAppearsInSupportedLocation_ShouldUseExpectedTargetType(String source, Class<?> type) {
        ReferenceUsage<?> usage = index().findBySource(DocumentPath.parse(source)).orElseThrow();

        assertEquals(type, usage.expectedType());
        assertInstanceOf(ResolvedReference.class, usage.resolution());
    }

    @Test
    void testBuild_WhenDocumentContainsAllModelBranches_ShouldIndexWithoutUnsupportedTypes() {
        ReferenceIndex index = assertDoesNotThrow(() -> new ReferenceIndexBuilder().build(
                DocumentIndex.build(ModelFixture.document())));

        assertFalse(index.usages().isEmpty());
        assertFalse(index.unresolvedUsages().isEmpty());
    }

    @Test
    void testReferencesTo_WhenComponentAndSchemaIdsShareLocation_ShouldReturnSameIncomingReferences() {
        DocumentIndex document = ReferenceFixture.index();
        ReferenceIndex index = new ReferenceIndexBuilder().build(document);
        ElementId componentId = document.entries().stream()
                .filter(entry -> entry.path().equals(DocumentPath.parse("/components/schemas/User")))
                .map(entry -> entry.value())
                .filter(value -> value instanceof SchemaComponent)
                .map(value -> ((SchemaComponent) value).getId())
                .findFirst().orElseThrow();
        Schema schema = (Schema) document.findByPath(DocumentPath.parse("/components/schemas/User")).orElseThrow().value();

        assertFalse(index.referencesTo(componentId).isEmpty());
        assertEquals(index.referencesTo(schema.getId()), index.referencesTo(componentId));
        assertTrue(index.referencesTo(ElementId.random()).isEmpty());
    }

    @Test
    void testReferencesTo_WhenReferenceUsesAlias_ShouldIndexIntermediateAndFinalTargets() {
        ReferenceIndex index = index();
        DocumentPath source = DocumentPath.parse("/paths/~1users/get/parameters/0/$ref");

        assertTrue(index.referencesTo(DocumentPath.parse("/components/parameters/Alias")).stream()
                .anyMatch(usage -> usage.sourcePath().equals(source)));
        assertTrue(index.referencesTo(DocumentPath.parse("/components/parameters/Limit")).stream()
                .anyMatch(usage -> usage.sourcePath().equals(source)));
    }

    @Test
    void testReferencesToSubtree_WhenTargetsAreNested_ShouldIncludeDescendantsButNotTextualPrefixes() {
        ReferenceIndex index = index();
        List<ReferenceUsage<?>> references = index.referencesToSubtree(DocumentPath.parse("/components/schemas/User"));

        assertTrue(references.stream().anyMatch(usage -> usage.reference().endsWith("/properties/name")));
        assertFalse(references.stream().anyMatch(usage -> usage.reference().equals("#/components/schemas/User2")));
        assertEquals(1, index.referencesTo(DocumentPath.parse("/components/schemas/User2")).size());
    }

    @Test
    void testReferencesFromSubtree_WhenSchemaHasCompositionAndMapping_ShouldReturnOnlyItsReferences() {
        ReferenceIndex index = index();
        DocumentPath derived = DocumentPath.parse("/components/schemas/Derived");

        List<ReferenceUsage<?>> outgoing = index.referencesFromSubtree(derived);

        assertEquals(3, outgoing.size());
        assertTrue(outgoing.stream().allMatch(usage -> usage.sourcePath().startsWith(derived)));
    }

    @Test
    void testBuild_WhenDiscriminatorUsesComponentName_ShouldResolveNameAndPreserveOriginalValue() {
        ReferenceUsage<?> usage = index().findBySource(DocumentPath.parse(
                "/components/schemas/Derived/discriminator/mapping/user")).orElseThrow();

        assertEquals(ReferenceKind.DISCRIMINATOR_MAPPING, usage.kind());
        assertEquals("User", usage.reference());
        ResolvedReference<?> resolved = assertInstanceOf(ResolvedReference.class, usage.resolution());
        assertEquals(DocumentPath.parse("/components/schemas/User"), resolved.targetPath());
    }

    @Test
    void testBuild_WhenExampleDataContainsRefKey_ShouldNotTreatDataAsReference() {
        ReferenceIndex index = index();

        assertFalse(index.usages().stream().anyMatch(usage -> usage.reference().equals("#/not-a-reference")));
        assertTrue(index.referencesFromSubtree(DocumentPath.parse("/components/schemas/User/example")).isEmpty());
    }

    @Test
    void testUnresolvedUsages_WhenTargetsAreMissingExternalOrCyclic_ShouldPreserveFailures() {
        ReferenceIndex index = index();

        List<ReferenceFailure> reasons = index.unresolvedUsages().stream()
                .map(usage -> ((UnresolvedReference<?>) usage.resolution()).reason()).toList();

        assertTrue(reasons.containsAll(List.of(ReferenceFailure.TARGET_NOT_FOUND,
                ReferenceFailure.EXTERNAL_REFERENCE, ReferenceFailure.CYCLIC_REFERENCE)));
        assertTrue(index.referencesTo(DocumentPath.parse("/components/parameters/Missing")).stream()
                .anyMatch(usage -> usage.sourcePath().equals(DocumentPath.parse("/components/parameters/BrokenAlias/$ref"))));
    }

    @Test
    void testUsages_WhenCallerAttemptsMutation_ShouldExposeReadOnlyResults() {
        ReferenceIndex index = index();

        assertThrows(UnsupportedOperationException.class, index.usages()::clear);
        assertThrows(UnsupportedOperationException.class, index.unresolvedUsages()::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> index.referencesTo(DocumentPath.parse("/components/schemas/User")).clear());
        assertTrue(index.findBySource(DocumentPath.parse("/missing/$ref")).isEmpty());
    }

    private static ReferenceIndex index() {
        return new ReferenceIndexBuilder().build(ReferenceFixture.index());
    }

    @Test
    void testBuild_WhenIndexesAreRebuilt_ShouldPreserveDocumentAndProduceSameUsages() {
        ApiDocument document = ModelFixture.document();
        ApiDocument snapshot = document.copy();
        ReferenceIndexBuilder builder = new ReferenceIndexBuilder();

        ReferenceIndex first = builder.build(DocumentIndex.build(document));
        ReferenceIndex second = builder.build(DocumentIndex.build(document));

        ModelAssertions.assertIndependentCopy(snapshot, document);
        assertEquals(first.usages(), second.usages());
    }

    private static Stream<Arguments> referenceSources() {
        return Stream.of(
                Arguments.of("/components/parameters/Alias/$ref", Parameter.class),
                Arguments.of("/components/requestBodies/AliasBody/$ref", RequestBody.class),
                Arguments.of("/components/responses/AliasResponse/$ref", ApiResponse.class),
                Arguments.of("/components/headers/AliasHeader/$ref", Header.class),
                Arguments.of("/components/examples/AliasExample/$ref", Example.class),
                Arguments.of("/components/securitySchemes/AliasKey/$ref", SecurityScheme.class),
                Arguments.of("/paths/~1users/parameters/0/$ref", Parameter.class),
                Arguments.of("/paths/~1users/get/parameters/0/$ref", Parameter.class),
                Arguments.of("/paths/~1users/get/requestBody/$ref", RequestBody.class),
                Arguments.of("/paths/~1users/get/responses/200/$ref", ApiResponse.class),
                Arguments.of("/components/responses/Success/headers/X-Trace/$ref", Header.class),
                Arguments.of("/components/responses/Success/content/application~1json/examples/sample/$ref", Example.class),
                Arguments.of("/components/responses/Success/content/application~1json/encoding/name/headers/X-Trace/$ref", Header.class),
                Arguments.of("/components/parameters/Limit/schema/$ref", Schema.class));
    }
}
