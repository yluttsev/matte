package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SchemaSerializerCoverageTest {

    private static final String COMPONENT_NAME = "Complete";
    private static final String ID_GETTER = "getId";
    private static final String TYPES_GETTER = "getTypes";
    private static final List<String> FIXTURES = List.of("additions-empty.yaml", "additions-empty-3.1.json");

    private final OpenApiImporter importer = new OpenApiImporter();
    private final OpenApiSchemaEditor editor = new OpenApiSchemaEditor();

    @Test
    void testAddComponent_WhenEverySchemaFieldIsPopulated_ShouldKeepEveryFieldAfterReimport() throws IOException {
        SchemaObject complete = completeSchema();
        assertEveryGetterIsPopulated(complete);

        for (String fixture : FIXTURES) {
            ImportResult imported = importer.importDocument(TestFixtures.bytes(fixture), ImportOptions.autoDetect());
            assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());

            OpenApiEditResult result = editor.addComponent(
                    imported,
                    new AddSchemaComponent(new SchemaComponent(ElementId.random(), COMPONENT_NAME, complete))
            );

            assertInstanceOf(EditSucceeded.class, result.edit(), result.edit().toString());
            ImportResult reimported = importer.importDocument(
                    result.source().orElseThrow().content(),
                    ImportOptions.withFormat(imported.format().orElseThrow())
            );
            assertEquals(ImportStatus.IMPORTED, reimported.status(), reimported.diagnostics().toString());
            assertSameStructure(complete, component(reimported.document().orElseThrow()), COMPONENT_NAME);
        }
    }

    private SchemaObject completeSchema() {
        return SchemaObject.builder(ElementId.random())
                .types(new LinkedHashSet<>(List.of(JsonType.OBJECT, JsonType.NULL)))
                .format("custom")
                .title("Complete title")
                .description("Complete description")
                .defaultValue(new StringValue("default"))
                .example(new StringValue("example"))
                .examples(List.of(new StringValue("first"), new StringValue("second")))
                .enumValues(List.of(new StringValue("a"), new StringValue("b")))
                .constValue(new StringValue("constant"))
                .deprecated(true)
                .readOnly(true)
                .writeOnly(false)
                .properties(List.of(
                        new SchemaProperty(
                                ElementId.random(),
                                "p1",
                                SchemaObject.builder(ElementId.random()).oneOf(List.of(leaf())).build()
                        ),
                        new SchemaProperty(
                                ElementId.random(),
                                "p2",
                                SchemaObject.builder(ElementId.random())
                                        .items(SchemaObject.builder(ElementId.random())
                                                .reference(new SchemaReference("#/components/schemas/Other"))
                                                .build())
                                        .build()
                        )
                ))
                .required(new LinkedHashSet<>(List.of("p1")))
                .additionalProperties(SchemaObject.builder(ElementId.random()).anyOf(List.of(leaf())).build())
                .minProperties(BigInteger.ONE)
                .maxProperties(BigInteger.TEN)
                .minItems(BigInteger.ONE)
                .maxItems(BigInteger.TEN)
                .uniqueItems(true)
                .minLength(BigInteger.ONE)
                .maxLength(BigInteger.TEN)
                .pattern("^a")
                .minimum(BigDecimal.ONE)
                .maximum(BigDecimal.TEN)
                .exclusiveMinimum(BigDecimal.ZERO)
                .exclusiveMaximum(new BigDecimal("11"))
                .multipleOf(new BigDecimal("2"))
                .allOf(List.of(leaf()))
                .not(leaf())
                .discriminator(Discriminator.builder()
                        .propertyName("p1")
                        .mapping(Map.of("a", new SchemaReference("#/components/schemas/Other")))
                        .build())
                .build();
    }

    private SchemaObject leaf() {
        return SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.STRING)).build();
    }

    private void assertEveryGetterIsPopulated(SchemaObject schema) {
        Set<String> populated = new LinkedHashSet<>();
        collectPopulatedGetters(schema, populated);
        for (Method getter : getters(SchemaObject.class)) {
            assertTrue(populated.contains(getter.getName()), "Test schema does not populate " + getter.getName());
        }
    }

    private void collectPopulatedGetters(SchemaObject schema, Set<String> populated) {
        for (Method getter : getters(SchemaObject.class)) {
            Object value = invoke(getter, schema);
            boolean empty = value == null || value instanceof Collection<?> collection && collection.isEmpty();
            if (!empty) {
                populated.add(getter.getName());
            }
        }
        List<Schema> children = new ArrayList<>(schema.getAllOf());
        children.addAll(schema.getOneOf());
        children.addAll(schema.getAnyOf());
        schema.getProperties().forEach(property -> children.add(property.getSchema()));
        children.add(schema.getItems());
        children.add(schema.getAdditionalProperties());
        children.add(schema.getNot());
        for (Schema child : children) {
            if (child instanceof SchemaObject object) {
                collectPopulatedGetters(object, populated);
            }
        }
    }

    private SchemaObject component(ApiDocument document) {
        return (SchemaObject) document.getComponents().getSchemas().stream()
                .filter(candidate -> candidate.getName().equals(COMPONENT_NAME))
                .findFirst()
                .orElseThrow()
                .getSchema();
    }

    private void assertSameStructure(Object expected, Object actual, String location) {
        if (expected == null || actual == null) {
            assertEquals(expected, actual, location);
        } else if (expected instanceof SchemaObject) {
            assertSameGetters(SchemaObject.class, expected, actual, location);
        } else if (expected instanceof SchemaProperty) {
            assertSameGetters(SchemaProperty.class, expected, actual, location);
        } else if (expected instanceof Discriminator) {
            assertSameGetters(Discriminator.class, expected, actual, location);
        } else if (expected instanceof BooleanSchema) {
            assertSameGetters(BooleanSchema.class, expected, actual, location);
        } else if (expected instanceof Collection<?> collection) {
            assertSameCollection(collection, (Collection<?>) actual, location);
        } else if (expected instanceof Map<?, ?> mapping) {
            assertSameMap(mapping, (Map<?, ?>) actual, location);
        } else if (expected instanceof BigDecimal number) {
            assertEquals(0, number.compareTo((BigDecimal) actual), location);
        } else {
            assertEquals(expected, actual, location);
        }
    }

    private void assertSameGetters(Class<?> type, Object expected, Object actual, String location) {
        assertEquals(expected.getClass(), actual.getClass(), location);
        for (Method getter : getters(type)) {
            String field = location + "." + getter.getName();
            assertSameStructure(invoke(getter, expected), invoke(getter, actual), field);
        }
    }

    private void assertSameCollection(Collection<?> expected, Collection<?> actual, String location) {
        assertEquals(expected.size(), actual.size(), location);
        if (location.endsWith(TYPES_GETTER)) {
            assertEquals(Set.copyOf(expected), Set.copyOf(actual), location);
            return;
        }
        List<?> expectedItems = new ArrayList<>(expected);
        List<?> actualItems = new ArrayList<>(actual);
        for (int index = 0; index < expectedItems.size(); index++) {
            assertSameStructure(expectedItems.get(index), actualItems.get(index), location + "[" + index + "]");
        }
    }

    private void assertSameMap(Map<?, ?> expected, Map<?, ?> actual, String location) {
        assertEquals(expected.keySet(), actual.keySet(), location);
        expected.forEach((key, value) -> assertSameStructure(value, actual.get(key), location + "." + key));
    }

    private List<Method> getters(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> method.getName().startsWith("get") && !method.getName().equals("getClass"))
                .filter(method -> !method.getName().equals(ID_GETTER))
                .filter(method -> !method.getReturnType().equals(void.class))
                .sorted(Comparator.comparing(Method::getName))
                .toList();
    }

    private Object invoke(Method getter, Object target) {
        try {
            return getter.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return fail("Cannot read " + getter.getName(), exception);
        }
    }
}
