package io.github.yluttsev.matte.core.editing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.change.SchemaObjectUpdated;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaObjectUpdateTest {

    private final DocumentEditor editor = new DocumentEditor();

    @Test
    void testApply_WhenUpdatingSchemaFields_ShouldSetReplaceAndClearWithoutLosingOtherValues() {
        SchemaProperty property = new SchemaProperty(
                ElementId.random(), "name", SchemaObject.builder(ElementId.random()).build());
        SchemaObject originalSchema = SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.STRING))
                .title("Before")
                .description("Keep this description")
                .minimum(new BigDecimal("1.0"))
                .defaultValue(new StringValue("old"))
                .properties(List.of(property))
                .build();
        ApiDocument original = document(originalSchema);
        SchemaObject replacement = SchemaObject.builder(originalSchema)
                .types(Set.of(JsonType.STRING, JsonType.NULL))
                .title("After")
                .minimum(null)
                .minLength(BigInteger.valueOf(2))
                .defaultValue(null)
                .example(new NullValue())
                .deprecated(Boolean.TRUE)
                .build();

        EditSucceeded result = assertInstanceOf(EditSucceeded.class,
                editor.apply(original, new UpdateSchemaObject(replacement)));

        SchemaObject updated = schema(result.document(), originalSchema.getId());
        assertEquals(Set.of(JsonType.STRING, JsonType.NULL), updated.getTypes());
        assertEquals("After", updated.getTitle());
        assertEquals("Keep this description", updated.getDescription());
        assertEquals(new BigInteger("2"), updated.getMinLength());
        assertNull(updated.getMinimum());
        assertNull(updated.getDefaultValue());
        assertEquals(new NullValue(), updated.getExample());
        assertEquals(Boolean.TRUE, updated.getDeprecated());
        assertEquals(property.getId(), updated.getProperties().getFirst().getId());
        assertNotSame(originalSchema, updated);

        SchemaObjectUpdated change = assertInstanceOf(
                SchemaObjectUpdated.class,
                result.changeSet().changes().getFirst());
        assertEquals(originalSchema.getId(), change.schemaId());
        assertEquals("After", change.after().getTitle());
        assertEquals("Before", change.before().getTitle());
    }

    @Test
    void testApply_WhenReplacementUsesIdFromOutsideTarget_ShouldRejectConflict() {
        SchemaObject target = SchemaObject.builder(ElementId.random()).build();
        ElementId externalId = ElementId.random();
        SchemaObject other = SchemaObject.builder(ElementId.random())
                .properties(List.of(new SchemaProperty(externalId, "value",
                        new BooleanSchema(ElementId.random(), true))))
                .build();
        ApiDocument original = ApiDocument.builder(ElementId.random()).components(Components.builder().schemas(List.of(
                new SchemaComponent(ElementId.random(), "Target", target),
                new SchemaComponent(ElementId.random(), "Other", other))).build()).build();
        SchemaObject replacement = SchemaObject.builder(target)
                .properties(List.of(new SchemaProperty(externalId, "value",
                        new BooleanSchema(ElementId.random(), false))))
                .build();

        EditRejected result = assertInstanceOf(EditRejected.class,
                editor.apply(original, new UpdateSchemaObject(replacement)));

        assertEquals(EditCode.ID_CONFLICT, result.issues().getFirst().code());
        assertTrue(schema(original, target.getId()).getProperties().isEmpty());
    }

    private static SchemaObject schema(ApiDocument document, ElementId id) {
        return (SchemaObject) DocumentIndex.build(document).findById(id).orElseThrow().value();
    }

    private static ApiDocument document(SchemaObject schema) {
        return ApiDocument.builder(ElementId.random()).components(Components.builder().schemas(List.of(
                new SchemaComponent(ElementId.random(), "User", schema))).build()).build();
    }
}
