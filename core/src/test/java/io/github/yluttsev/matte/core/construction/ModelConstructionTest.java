package io.github.yluttsev.matte.core.construction;

import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.path.HttpMethod;
import io.github.yluttsev.matte.core.model.path.Operation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.reference.InlineObject;
import io.github.yluttsev.matte.core.model.response.ResponseKey;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelConstructionTest {

    @Test
    void testBuild_WhenDocumentIsPopulated_ShouldPreserveConfiguredFields() {
        ApiDocument document = ModelFixture.document();
        PathItem path = document.getPaths().getFirst();
        Operation operation = path.getOperations().get(HttpMethod.GET);

        assertAll(
                () -> assertEquals("Users API", document.getInfo().getTitle()),
                () -> assertEquals("API summary", document.getInfo().getSummary()),
                () -> assertEquals("API description", document.getInfo().getDescription()),
                () -> assertEquals("1.2", document.getInfo().getVersion()),
                () -> assertEquals("https://example.test/terms", document.getInfo().getTermsOfService()),
                () -> assertEquals("support@example.test", document.getInfo().getContact().email()),
                () -> assertEquals("Apache-2.0", document.getInfo().getLicense().identifier()),
                () -> assertEquals("https://example.test/docs", document.getExternalDocs().url()),
                () -> assertEquals("users", document.getTags().getFirst().getName()),
                () -> assertEquals("/users", path.getPath()),
                () -> assertEquals("Users", path.getSummary()),
                () -> assertEquals("Users resource", path.getDescription()),
                () -> assertEquals("getUsers", operation.getOperationId()),
                () -> assertEquals("Get users", operation.getSummary()),
                () -> assertEquals("Returns users", operation.getDescription()),
                () -> assertTrue(operation.getDeprecated()),
                () -> assertEquals(List.of("users", "public"), operation.getTags()),
                () -> assertNotNull(operation.getRequestBody()),
                () -> assertEquals(List.of(new ResponseKey("200"), new ResponseKey("2XX"), new ResponseKey("default")),
                        List.copyOf(operation.getResponses().keySet())),
                () -> assertInstanceOf(InlineObject.class, operation.getResponses().get(new ResponseKey("200"))),
                () -> assertEquals(List.of("read", "write"), document.getSecurity().getFirst().getSchemes().get("OAuth")),
                () -> assertEquals("dev", document.getServers().getFirst().getVariables().get("environment").getDefaultValue()),
                () -> assertEquals(1, document.getComponents().getSchemas().size()),
                () -> assertEquals(1, document.getComponents().getParameters().size()),
                () -> assertEquals(1, document.getComponents().getRequestBodies().size()),
                () -> assertEquals(1, document.getComponents().getResponses().size()),
                () -> assertEquals(1, document.getComponents().getHeaders().size()),
                () -> assertEquals(1, document.getComponents().getExamples().size()),
                () -> assertEquals(1, document.getComponents().getSecuritySchemes().size()));
    }

    @Test
    void testBuild_WhenSchemaFieldsAreCombined_ShouldPreserveAllFields() {
        SchemaObject schema = ModelFixture.schema();

        assertAll(
                () -> assertEquals(Set.of(JsonType.OBJECT, JsonType.NULL), schema.getTypes()),
                () -> assertEquals("custom", schema.getFormat()),
                () -> assertEquals("User schema", schema.getTitle()),
                () -> assertEquals("Schema description", schema.getDescription()),
                () -> assertEquals(new SchemaReference("#/components/schemas/Base"), schema.getReference()),
                () -> assertEquals(new NullValue(), schema.getDefaultValue()),
                () -> assertEquals(ModelFixture.value(), schema.getExample()),
                () -> assertEquals(List.of(ModelFixture.value(), new StringValue("other")), schema.getExamples()),
                () -> assertEquals(2, schema.getEnumValues().size()),
                () -> assertNotNull(schema.getConstValue()),
                () -> assertTrue(schema.getDeprecated()),
                () -> assertTrue(schema.getReadOnly()),
                () -> assertTrue(schema.getWriteOnly()),
                () -> assertEquals("name", schema.getProperties().getFirst().getName()),
                () -> assertEquals(Set.of("name", "notYetDefined"), schema.getRequired()),
                () -> assertNotNull(schema.getAdditionalProperties()),
                () -> assertEquals(BigInteger.ONE, schema.getMinProperties()),
                () -> assertEquals(BigInteger.TEN, schema.getMaxProperties()),
                () -> assertNotNull(schema.getItems()),
                () -> assertEquals(BigInteger.TWO, schema.getMinItems()),
                () -> assertEquals(BigInteger.valueOf(20), schema.getMaxItems()),
                () -> assertTrue(schema.getUniqueItems()),
                () -> assertEquals(BigInteger.valueOf(3), schema.getMinLength()),
                () -> assertEquals(BigInteger.valueOf(30), schema.getMaxLength()),
                () -> assertEquals("[a-z]+", schema.getPattern()),
                () -> assertEquals(new BigDecimal("1.1"), schema.getMinimum()),
                () -> assertEquals(new BigDecimal("10.1"), schema.getMaximum()),
                () -> assertEquals(new BigDecimal("0.1"), schema.getExclusiveMinimum()),
                () -> assertEquals(new BigDecimal("11.1"), schema.getExclusiveMaximum()),
                () -> assertEquals(new BigDecimal("0.5"), schema.getMultipleOf()),
                () -> assertEquals(1, schema.getAllOf().size()),
                () -> assertEquals(1, schema.getOneOf().size()),
                () -> assertEquals(1, schema.getAnyOf().size()),
                () -> assertNotNull(schema.getNot()),
                () -> assertEquals("kind", schema.getDiscriminator().getPropertyName()),
                () -> assertEquals(new SchemaReference("#/components/schemas/User"),
                        schema.getDiscriminator().getMapping().get("user")));
    }

    @Test
    void testBuild_WhenDocumentIsUnfinishedOrSemanticallyInvalid_ShouldAllowConstruction() {
        Parameter parameter = Parameter.builder(ElementId.random()).build();
        Operation operation = Operation.builder(ElementId.random())
                .parameters(List.of(new InlineObject<>(parameter)))
                .build();

        ApiDocument document = assertDoesNotThrow(() -> ApiDocument.builder(ElementId.random())
                .paths(List.of(PathItem.builder(ElementId.random(), "")
                        .operations(Map.of(HttpMethod.GET, operation))
                        .build()))
                .build());

        assertNull(document.getInfo());
        assertTrue(document.getPaths().getFirst().getOperations().get(HttpMethod.GET).getResponses().isEmpty());
    }

    @Test
    void testBuild_WhenValuesAreAbsentOrExplicitNull_ShouldPreserveDistinction() {
        SchemaObject absent = SchemaObject.builder(ElementId.random()).build();
        SchemaObject explicit = SchemaObject.builder(ElementId.random())
                .defaultValue(new NullValue())
                .example(new NullValue())
                .constValue(new NullValue())
                .enumValues(List.of(new NullValue()))
                .build();

        assertAll(
                () -> assertNull(absent.getDefaultValue()),
                () -> assertNull(absent.getExample()),
                () -> assertNull(absent.getConstValue()),
                () -> assertInstanceOf(NullValue.class, explicit.getDefaultValue()),
                () -> assertInstanceOf(NullValue.class, explicit.getExample()),
                () -> assertInstanceOf(NullValue.class, explicit.getConstValue()),
                () -> assertInstanceOf(NullValue.class, explicit.getEnumValues().getFirst()),
                () -> assertInstanceOf(NullValue.class,
                        Example.builder(ElementId.random()).value(new NullValue()).build().getValue()));
    }

    @Test
    void testBuild_WhenSecurityIsNotSpecified_ShouldInheritSecurity() {
        Operation operation = Operation.builder(ElementId.random()).build();

        assertFalse(operation.hasSecurityOverride());
        assertTrue(operation.getSecurity().isEmpty());
    }

    @Test
    void testCopy_WhenSecurityOverrideIsEmpty_ShouldPreserveExplicitOverride() {
        Operation operation = Operation.builder(ElementId.random()).security(List.of()).build().copy();

        assertTrue(operation.hasSecurityOverride());
        assertTrue(operation.getSecurity().isEmpty());
    }

    @Test
    void testInheritSecurity_WhenOverrideExists_ShouldClearOverrideAndRequirements() {
        Operation operation = Operation.builder(ElementId.random())
                .security(ModelFixture.document().getSecurity())
                .inheritSecurity()
                .build()
                .copy();

        assertFalse(operation.hasSecurityOverride());
        assertTrue(operation.getSecurity().isEmpty());
    }
}
