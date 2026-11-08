package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.value.ArrayValue;
import io.github.yluttsev.matte.core.model.value.NumberValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class SchemaNumberPrecisionTest {

    private static final BigDecimal PRECISE = new BigDecimal("0.12345678901234567890123456789");
    private static final BigInteger ABOVE_INTEGER = new BigInteger("2147483648");
    private static final BigInteger VERY_LARGE = new BigInteger("999999999999999999999");

    private final OpenApiImporter importer = new OpenApiImporter();

    @Test
    void testImportDocument_WhenJsonHasPreciseNestedNumbersAndLargeConstraints_ShouldPreserveValues()
            throws IOException {
        assertNumericFixture("schema-numbers-3.1.json");
    }

    @Test
    void testImportDocument_WhenYamlHasPreciseNestedNumbersAndLargeConstraints_ShouldPreserveValues()
            throws IOException {
        assertNumericFixture("schema-numbers-3.1.yaml");
    }

    @Test
    void testImportDocument_WhenConstraintIsNegative_ShouldPreserveExistingParserBehavior() throws IOException {
        ImportResult imported = importFixture("schema-negative-constraint.yaml");

        assertEquals(BigInteger.valueOf(-1), schema(imported).getMinLength());
    }

    @Test
    void testImportDocument_WhenConstraintIsFractional_ShouldPreserveExistingParserBehavior() throws IOException {
        ImportResult imported = importFixture("schema-invalid-constraint.yaml");

        assertNull(schema(imported).getMinLength());
    }

    @Test
    void testUpdateSchema_WhenJsonContainsPreciseValuesAndLargeConstraint_ShouldRoundTrip() throws IOException {
        ImportResult imported = importFixture("schema-field-editing-3.1.json");
        SchemaObject replacement = SchemaObject.builder(schema(imported))
                .minLength(ABOVE_INTEGER)
                .defaultValue(new ObjectValue(Map.of("nested", new ArrayValue(List.of(new NumberValue(PRECISE))))))
                .build();

        OpenApiEditResult edited = new OpenApiEditor().updateSchema(
                imported,
                new UpdateSchemaObject(replacement)
        );
        ImportResult reimported = importer.importDocument(
                edited.source().orElseThrow().content(),
                ImportOptions.autoDetect()
        );

        assertInstanceOf(OpenApiEditResult.Applied.class, edited);
        assertEquals(ABOVE_INTEGER, schema(edited.state()).getMinLength());
        assertEquals(replacement.getDefaultValue(), schema(edited.state()).getDefaultValue());
        assertEquals(ABOVE_INTEGER, schema(reimported).getMinLength());
        assertEquals(replacement.getDefaultValue(), schema(reimported).getDefaultValue());
    }

    @Test
    void testUpdateSchema_WhenOriginalConstraintExceedsIntegerRange_ShouldRetainIt() throws IOException {
        ImportResult imported = importFixture("schema-numbers-3.1.yaml");
        SchemaObject replacement = SchemaObject.builder(schema(imported)).title("Updated").build();

        OpenApiEditResult edited = new OpenApiEditor().updateSchema(
                imported,
                new UpdateSchemaObject(replacement)
        );
        ImportResult reimported = importer.importDocument(
                edited.source().orElseThrow().content(),
                ImportOptions.autoDetect()
        );

        assertInstanceOf(OpenApiEditResult.Applied.class, edited);
        assertEquals(ABOVE_INTEGER, schema(edited.state()).getMinLength());
        assertEquals(ABOVE_INTEGER, schema(reimported).getMinLength());
    }

    private void assertNumericFixture(String name) throws IOException {
        SchemaObject schema = schema(importFixture(name));
        NumberValue precise = new NumberValue(PRECISE);
        ObjectValue nestedArray = new ObjectValue(Map.of(
                "nested",
                new ArrayValue(List.of(precise, new NumberValue(new BigDecimal(VERY_LARGE))))
        ));
        ObjectValue nestedNumber = new ObjectValue(Map.of("nested", precise));

        assertEquals(BigInteger.valueOf(3), schema.getMinProperties());
        assertEquals(BigInteger.valueOf(Integer.MAX_VALUE), schema.getMaxProperties());
        assertEquals(ABOVE_INTEGER, schema.getMinItems());
        assertEquals(VERY_LARGE, schema.getMaxItems());
        assertEquals(ABOVE_INTEGER, schema.getMinLength());
        assertEquals(VERY_LARGE, schema.getMaxLength());
        assertEquals(nestedArray, schema.getDefaultValue());
        assertEquals(precise, schema.getExample());
        assertEquals(List.of(nestedNumber), schema.getExamples());
        assertEquals(List.of(precise), schema.getEnumValues());
        assertEquals(new ObjectValue(Map.of("nested", new ArrayValue(List.of(precise)))), schema.getConstValue());
        SchemaObject property = assertInstanceOf(SchemaObject.class, schema.getProperties().getFirst().getSchema());
        SchemaObject branch = assertInstanceOf(SchemaObject.class, schema.getAllOf().getFirst());
        assertEquals(ABOVE_INTEGER, property.getMinLength());
        assertEquals(ABOVE_INTEGER, branch.getMaxProperties());
    }

    private ImportResult importFixture(String name) throws IOException {
        ImportResult imported = importer.importDocument(TestFixtures.bytes(name), ImportOptions.autoDetect());
        assertEquals(ImportStatus.IMPORTED, imported.status(), imported.diagnostics().toString());
        return imported;
    }

    private SchemaObject schema(ImportResult imported) {
        return assertInstanceOf(
                SchemaObject.class,
                imported.document().orElseThrow().getComponents().getSchemas().getFirst().getSchema()
        );
    }
}
