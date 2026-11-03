package io.github.yluttsev.matte.core.editing;

import java.lang.reflect.Modifier;
import java.util.List;
import io.github.yluttsev.matte.core.construction.ModelAssertions;
import io.github.yluttsev.matte.core.construction.ModelFixture;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.reference.InlineObject;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MutationAccessTest {

    @Test
    void testRequire_WhenModelBelongsToDifferentGraph_ShouldRejectDespiteMatchingIds() {
        ApiDocument original = ModelFixture.document();
        ApiDocument working = original.copy();
        MutationAccess access = new MutationAccess(working);

        assertThrows(IllegalStateException.class, () -> original.getComponents().addSchemaComponent(access,
                new SchemaComponent(ElementId.random(), "Other", new BooleanSchema(ElementId.random(), true))));
        assertThrows(
                IllegalStateException.class,
                () -> original.getComponents().getSchemas().getFirst().rename(access, "Other")
        );
        assertThrows(IllegalStateException.class, () -> ((SchemaObject) original.getComponents().getSchemas().getFirst()
                .getSchema()).addProperty(access, property()));
        access.close();
    }

    @Test
    void testClose_WhenSessionEnds_ShouldPreventFurtherMutation() {
        ApiDocument working = ModelFixture.document();
        MutationAccess access = new MutationAccess(working);
        SchemaComponent component = working.getComponents().getSchemas().getFirst();
        component.rename(access, "Customer");

        access.close();

        assertThrows(IllegalStateException.class, () -> component.rename(access, "Client"));
        assertThrows(IllegalStateException.class,
                () -> ((SchemaObject) component.getSchema()).rewriteReferences(access, value -> "changed"));
        assertEquals("Customer", component.getName());
    }

    @Test
    void testConstructor_WhenCalledOutsideEditingPackage_ShouldNotExposePublicConstructor() {
        assertEquals(0, MutationAccess.class.getConstructors().length);
        assertFalse(Modifier.isPublic(MutationAccess.class.getDeclaredConstructors()[0].getModifiers()));
    }

    @Test
    void testRequire_WhenNoAccessIsProvided_ShouldRejectMutation() {
        ApiDocument document = ModelFixture.document();
        SchemaComponent component = document.getComponents().getSchemas().getFirst();

        assertThrows(NullPointerException.class, () -> component.rename(null, "Customer"));
        assertThrows(NullPointerException.class, () -> document.getComponents().addSchemaComponent(null, component));
        assertThrows(NullPointerException.class,
                () -> ((SchemaObject) component.getSchema()).addProperty(null, property()));
    }

    @Test
    void testRewriteReferences_WhenTransformationFails_ShouldNotAffectOriginalGraph() {
        ApiDocument original = EditingFixture.document();
        ApiDocument snapshot = original.copy();
        ApiDocument working = original.copy();
        MutationAccess access = new MutationAccess(working);
        SchemaComponent component = working.getComponents().getSchemas().getFirst();
        component.rename(access, "Customer");
        IllegalStateException failure = new IllegalStateException("Unexpected transformation failure");

        try {
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> ((SchemaObject) component.getSchema()).rewriteReferences(access, value -> {
                        throw failure;
                    })));
        } finally {
            access.close();
        }

        ModelAssertions.assertIndependentCopy(snapshot, original);
        assertEquals("User", original.getComponents().getSchemas().getFirst().getName());
    }

    @Test
    void testValue_WhenReferenceChanges_ShouldPreserveIdAndAnnotations() {
        ObjectReference<Parameter> reference = new ObjectReference<>(
                ElementId.random(),
                "#/old",
                "summary",
                "description"
        );

        ObjectReference<?> updated = (ObjectReference<?>) ReferenceUpdates.value(reference, value -> "#/new");

        assertEquals(reference.id(), updated.id());
        assertEquals(reference.summary(), updated.summary());
        assertEquals(reference.description(), updated.description());
        assertEquals("#/old", reference.value());
        assertEquals("#/new", updated.value());
    }

    @Test
    void testValue_WhenReferenceIsUnchangedOrObjectIsInline_ShouldPreserveInstance() {
        ObjectReference<Parameter> reference = new ObjectReference<>(ElementId.random(), "#/old", null, null);
        InlineObject<Parameter> inline = new InlineObject<>(new Parameter(ElementId.random()));

        assertSame(reference, ReferenceUpdates.value(reference, value -> value));
        assertSame(inline, ReferenceUpdates.value(inline, value -> "#/new"));
    }

    @Test
    void testConstructor_WhenRejectedResultHasNoIssues_ShouldRejectInvalidResult() {
        assertThrows(IllegalArgumentException.class, () -> new EditRejected(List.of()));
    }

    private static SchemaProperty property() {
        return new SchemaProperty(ElementId.random(), "value", new BooleanSchema(ElementId.random(), true));
    }
}
