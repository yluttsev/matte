package io.github.yluttsev.matte.core.editing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.github.yluttsev.matte.core.construction.ModelAssertions;
import io.github.yluttsev.matte.core.editing.change.ReferenceChanged;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentRenamed;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.NamedComponent;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.path.HttpMethod;
import io.github.yluttsev.matte.core.model.path.Operation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.core.reference.ReferenceIndex;
import io.github.yluttsev.matte.core.reference.ReferenceIndexBuilder;
import io.github.yluttsev.matte.core.reference.ReferenceKind;
import io.github.yluttsev.matte.core.reference.ReferenceUsage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaComponentRenameTest {

    private final DocumentEditor editor = new DocumentEditor();

    @Test
    void testApply_WhenMappingMatchesAnotherSchemaName_ShouldNotRewriteMapping() {
        String name = "#/components/schemas/A";
        SchemaComponent target = new SchemaComponent(
                ElementId.random(),
                "A",
                SchemaObject.builder(ElementId.random()).build()
        );
        SchemaObject other = SchemaObject.builder(ElementId.random()).discriminator(Discriminator.builder()
                .mapping(Map.of("key", new SchemaReference(name))).build()).build();
        SchemaComponent namedLikePointer = new SchemaComponent(ElementId.random(), name, other);
        ApiDocument document = ApiDocument.builder(ElementId.random()).components(
                Components.builder().schemas(List.of(target, namedLikePointer)).build()).build();

        EditSucceeded result = assertInstanceOf(
                EditSucceeded.class,
                editor.apply(document, new RenameSchemaComponent(target.getId(), "B"))
        );

        SchemaObject updated = (SchemaObject) result.document().getComponents().getSchemas().get(1).getSchema();
        assertEquals(name, updated.getDiscriminator().getMapping().get("key").value());
        assertEquals(1, result.changeSet().changes().size());
        assertEquals(name, other.getDiscriminator().getMapping().get("key").value());
    }

    @Test
    void testApply_WhenRenamingComponent_ShouldPreserveIdsOrderAndOriginalDocument() {
        ApiDocument original = EditingFixture.document();
        ApiDocument snapshot = original.copy();
        SchemaComponent component = original.getComponents().getSchemas().getFirst();

        EditSucceeded result = assertInstanceOf(EditSucceeded.class,
                editor.apply(original, new RenameSchemaComponent(component.getId(), "Customer")));

        SchemaComponent renamed = result.document().getComponents().getSchemas().getFirst();
        assertEquals(component.getId(), renamed.getId());
        assertEquals(component.getSchema().getId(), renamed.getSchema().getId());
        assertEquals("Customer", renamed.getName());
        assertEquals("User title", ((SchemaObject) renamed.getSchema()).getTitle());
        assertEquals(original.getComponents().getSchemas().stream().map(SchemaComponent::getId).toList(),
                result.document().getComponents().getSchemas().stream().map(SchemaComponent::getId).toList());
        assertTrue(DocumentIndex.build(result.document())
                .findByPath(DocumentPath.parse("/components/schemas/User")).isEmpty());
        SchemaComponentRenamed change = assertInstanceOf(
                SchemaComponentRenamed.class,
                result.changeSet().changes().getFirst()
        );
        assertEquals(new SchemaComponentRenamed(component.getId(), "User", "Customer"), change);
        ModelAssertions.assertIndependentCopy(snapshot, original);
    }

    @Test
    void testApply_WhenReferencesExistInAllSupportedLocations_ShouldRewriteExactlyApplicableReferences() {
        ApiDocument original = EditingFixture.document();
        ReferenceIndex before = references(original);
        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(original,
                new RenameSchemaComponent(original.getComponents().getSchemas().getFirst().getId(), "Customer")));
        ReferenceIndex after = references(result.document());
        List<ReferenceChanged> expected = new ArrayList<>();

        for (ReferenceUsage<?> usage : before.usages()) {
            String updated = expectedReference(usage);
            DocumentPath sourceAfter = relocated(usage.sourcePath());
            ReferenceUsage<?> actual = after.findBySource(sourceAfter).orElseThrow();
            assertEquals(updated, actual.reference(), usage.sourcePath().toString());
            assertEquals(usage.kind(), actual.kind());
            assertEquals(usage.expectedType(), actual.expectedType());
            if (!usage.reference().equals(updated)) {
                expected.add(new ReferenceChanged(
                        usage.sourcePath(),
                        sourceAfter,
                        usage.reference(),
                        updated,
                        usage.kind()
                ));
            }
        }

        assertFalse(expected.isEmpty());
        assertEquals(before.usages().size(), after.usages().size());
        assertEquals(
                expected,
                result.changeSet().changes().stream().filter(ReferenceChanged.class::isInstance).toList()
        );
        SchemaObject renamed = (SchemaObject) result.document().getComponents().getSchemas().getFirst().getSchema();
        assertEquals(
                new ObjectValue(Map.of("$ref", new StringValue("#/components/schemas/User"))),
                renamed.getExample()
        );
        assertTrue(result.changeSet().changes().stream().filter(ReferenceChanged.class::isInstance)
                .map(ReferenceChanged.class::cast)
                .anyMatch(change -> !change.sourceBefore().equals(change.sourceAfter())));
    }

    @Test
    void testApply_WhenNameIsUnchanged_ShouldReturnIsolatedCopyAndEmptyChangeSet() {
        ApiDocument original = EditingFixture.document();

        EditSucceeded result = assertInstanceOf(EditSucceeded.class, editor.apply(original,
                new RenameSchemaComponent(original.getComponents().getSchemas().getFirst().getId(), "User")));

        assertTrue(result.changeSet().changes().isEmpty());
        ModelAssertions.assertIndependentCopy(original, result.document());
    }

    @Test
    void testApply_WhenNewNameExists_ShouldRejectWithoutChangingNamesOrReferences() {
        ApiDocument original = EditingFixture.document();
        ApiDocument snapshot = original.copy();

        EditRejected result = assertInstanceOf(EditRejected.class, editor.apply(original,
                new RenameSchemaComponent(original.getComponents().getSchemas().getFirst().getId(), "User2")));

        assertEquals(EditCode.NAME_CONFLICT, result.issues().getFirst().code());
        ModelAssertions.assertIndependentCopy(snapshot, original);
    }

    @Test
    void testApply_WhenNamesContainReservedCharacters_ShouldRewriteEscapedPointers() {
        String oldName = "A/B~ C%";
        String newName = "D+E#имя";
        DocumentPath oldPath = DocumentPath.root().child("components").child("schemas").child(oldName);
        DocumentPath newPath = DocumentPath.root().child("components").child("schemas").child(newName);
        SchemaObject schema = SchemaObject.builder(ElementId.random())
                .reference(new SchemaReference(oldPath.toFragment())).build();
        SchemaComponent component = new SchemaComponent(ElementId.random(), oldName, schema);
        ApiDocument original = ApiDocument.builder(ElementId.random()).components(
                Components.builder().schemas(List.of(component)).build()).build();

        EditSucceeded result = assertInstanceOf(EditSucceeded.class,
                editor.apply(original, new RenameSchemaComponent(component.getId(), newName)));

        SchemaObject edited = (SchemaObject) result.document().getComponents().getSchemas().getFirst().getSchema();
        assertEquals(newPath.toFragment(), edited.getReference().value());
        ReferenceChanged change = assertInstanceOf(ReferenceChanged.class, result.changeSet().changes().get(1));
        assertEquals(oldPath.child("$ref"), change.sourceBefore());
        assertEquals(newPath.child("$ref"), change.sourceAfter());
    }

    @Test
    void testApply_WhenReferenceReachesTargetThroughAlias_ShouldRewriteOnlyDirectReference() {
        SchemaComponent schema = new SchemaComponent(
                ElementId.random(),
                "User",
                SchemaObject.builder(ElementId.random()).build()
        );
        ObjectReference<Parameter> direct = new ObjectReference<>(
                ElementId.random(),
                "#/components/schemas/User",
                "summary",
                "description"
        );
        NamedComponent<ReferenceOr<Parameter>> alias = new NamedComponent<>(ElementId.random(), "Alias", direct);
        ObjectReference<Parameter> indirect = new ObjectReference<>(
                ElementId.random(),
                "#/components/parameters/Alias",
                null,
                null
        );
        ApiDocument original = ApiDocument.builder(ElementId.random()).components(
                Components.builder().schemas(List.of(schema)).parameters(List.of(alias)).build())
                .paths(List.of(PathItem.builder(ElementId.random(), "/users").operations(Map.of(HttpMethod.GET,
                        Operation.builder(ElementId.random()).parameters(List.of(indirect)).build())).build())).build();

        EditSucceeded result = assertInstanceOf(EditSucceeded.class,
                editor.apply(original, new RenameSchemaComponent(schema.getId(), "Customer")));

        ObjectReference<?> edited = (ObjectReference<?>) result.document().getComponents()
                .getParameters().getFirst().getValue();
        assertEquals("#/components/schemas/Customer", edited.value());
        assertEquals(direct.id(), edited.id());
        assertEquals("summary", edited.summary());
        assertEquals("description", edited.description());
        ObjectReference<?> unchanged = (ObjectReference<?>) result.document().getPaths().getFirst().getOperations()
                .get(HttpMethod.GET).getParameters().getFirst();
        assertEquals(indirect.value(), unchanged.value());
        assertEquals(2, result.changeSet().changes().size());
    }

    @Test
    void testApply_WhenResultIsEditedAgain_ShouldLeavePreviousResultUnchanged() {
        ApiDocument original = EditingFixture.document();
        ElementId id = original.getComponents().getSchemas().getFirst().getId();
        EditSucceeded first = assertInstanceOf(
                EditSucceeded.class,
                editor.apply(original, new RenameSchemaComponent(id, "Customer"))
        );
        ApiDocument firstSnapshot = first.document().copy();

        EditSucceeded second = assertInstanceOf(
                EditSucceeded.class,
                editor.apply(first.document(), new RenameSchemaComponent(id, "Client"))
        );

        assertEquals("Client", second.document().getComponents().getSchemas().getFirst().getName());
        ModelAssertions.assertIndependentCopy(firstSnapshot, first.document());
    }

    private static ReferenceIndex references(ApiDocument document) {
        return new ReferenceIndexBuilder().build(DocumentIndex.build(document));
    }

    private static String expectedReference(ReferenceUsage<?> usage) {
        String oldPrefix = "#/components/schemas/User";
        if (usage.kind() == ReferenceKind.DISCRIMINATOR_MAPPING && usage.reference().equals("User")) {
            return "Customer";
        }
        return usage.reference().equals(oldPrefix) || usage.reference().startsWith(oldPrefix + "/")
                ? "#/components/schemas/Customer" + usage.reference().substring(oldPrefix.length()) : usage.reference();
    }

    private static DocumentPath relocated(DocumentPath path) {
        DocumentPath prefix = DocumentPath.parse("/components/schemas/User");
        if (!path.startsWith(prefix)) {
            return path;
        }
        List<String> segments = new ArrayList<>(path.segments());
        segments.set(2, "Customer");
        return new DocumentPath(segments);
    }
}
