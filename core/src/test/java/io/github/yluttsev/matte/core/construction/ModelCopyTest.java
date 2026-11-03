package io.github.yluttsev.matte.core.construction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.NamedComponent;
import io.github.yluttsev.matte.core.model.info.ApiInfo;
import io.github.yluttsev.matte.core.model.media.Encoding;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.path.HttpMethod;
import io.github.yluttsev.matte.core.model.path.Operation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.reference.InlineObject;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.security.OAuthFlow;
import io.github.yluttsev.matte.core.model.security.OAuthFlows;
import io.github.yluttsev.matte.core.model.security.SecurityRequirement;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;
import io.github.yluttsev.matte.core.model.server.Server;
import io.github.yluttsev.matte.core.model.server.ServerVariable;
import io.github.yluttsev.matte.core.model.tag.Tag;
import io.github.yluttsev.matte.core.model.value.ArrayValue;
import io.github.yluttsev.matte.core.model.value.DocumentValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelCopyTest {

    @Test
    void testCopy_WhenDocumentIsPopulated_ShouldCopyOwnedNodesAndPreserveFieldsIdsAndOrder() {
        ApiDocument document = ModelFixture.document();

        ModelAssertions.assertIndependentCopy(document, document.copy());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builders")
    void testBuild_WhenBuilderIsReused_ShouldProduceIndependentGraphs(
            String name,
            Supplier<? extends CopyableModel<?>> build
    ) {
        ModelAssertions.assertIndependentCopy(build.get(), build.get());
    }

    @Test
    void testBuild_WhenBuilderChangesAfterBuild_ShouldLeavePreviousModelUnchanged() {
        SchemaObject.Builder builder = SchemaObject.builder(ElementId.random()).title("Original");
        SchemaObject original = builder.build();

        SchemaObject changed = builder.title("Changed").build();

        assertEquals("Original", original.getTitle());
        assertEquals("Changed", changed.getTitle());
        assertEquals(original.getId(), changed.getId());
    }

    @Test
    void testPaths_WhenInputListChangesBeforeBuild_ShouldPreserveSnapshot() {
        List<PathItem> paths = new ArrayList<>(List.of(new PathItem(ElementId.random(), "/users")));
        ApiDocument.Builder builder = ApiDocument.builder(ElementId.random()).paths(paths);

        paths.clear();
        ApiDocument document = builder.build();
        builder.paths(List.of());

        assertEquals("/users", document.getPaths().getFirst().getPath());
    }

    @Test
    void testOperations_WhenInputMapChangesBeforeBuild_ShouldPreserveSnapshotAndOrder() {
        Map<HttpMethod, Operation> operations = new LinkedHashMap<>();
        operations.put(HttpMethod.POST, Operation.builder(ElementId.random()).summary("Create").build());
        operations.put(HttpMethod.GET, Operation.builder(ElementId.random()).summary("Read").build());
        PathItem.Builder builder = PathItem.builder(ElementId.random(), "/users").operations(operations);

        operations.clear();
        PathItem path = builder.build();
        builder.operations(Map.of());

        assertEquals(List.of(HttpMethod.POST, HttpMethod.GET), List.copyOf(path.getOperations().keySet()));
        assertEquals("Create", path.getOperations().get(HttpMethod.POST).getSummary());
    }

    @Test
    void testSchemes_WhenInputMapAndScopeListsChange_ShouldPreserveSnapshot() {
        List<String> scopes = new ArrayList<>(List.of("read", "write"));
        Map<String, List<String>> schemes = new LinkedHashMap<>();
        schemes.put("oauth", scopes);
        SecurityRequirement.Builder builder = SecurityRequirement.builder().schemes(schemes);

        scopes.clear();
        schemes.clear();
        SecurityRequirement requirement = builder.build();
        builder.schemes(Map.of());

        assertEquals(List.of("read", "write"), requirement.getSchemes().get("oauth"));
    }

    @Test
    void testConstructor_WhenOwnedModelsAreProvided_ShouldCopyOwnedModels() {
        SchemaObject schema = ModelFixture.schema();
        SchemaProperty property = new SchemaProperty(ElementId.random(), "user", schema);
        SchemaComponent component = new SchemaComponent(ElementId.random(), "User", schema);
        Parameter parameter = ModelFixture.parameter();
        InlineObject<Parameter> inline = new InlineObject<>(parameter);
        NamedComponent<ReferenceOr<Parameter>> named = new NamedComponent<>(ElementId.random(), "Limit", inline);

        ModelAssertions.assertIndependentCopy(schema, property.getSchema());
        ModelAssertions.assertIndependentCopy(schema, component.getSchema());
        ModelAssertions.assertIndependentCopy(parameter, inline.value());
        ModelAssertions.assertIndependentCopy(inline, named.getValue());
    }

    @Test
    void testGetters_WhenDocumentIsPopulated_ShouldExposeOnlyReadOnlyCollections() {
        ModelAssertions.assertReadOnlyCollections(ModelFixture.document());
    }

    @Test
    void testConstructor_WhenDataValueContainersChange_ShouldPreserveReadOnlySnapshots() {
        List<DocumentValue> input = new ArrayList<>(List.of(new StringValue("Ada")));
        ArrayValue array = new ArrayValue(input);
        Map<String, DocumentValue> properties = new LinkedHashMap<>();
        properties.put("names", array);
        properties.put("other", new StringValue("Bob"));
        ObjectValue object = new ObjectValue(properties);

        input.clear();
        properties.clear();

        assertEquals(List.of(new StringValue("Ada")), array.values());
        assertEquals(List.of("names", "other"), List.copyOf(object.properties().keySet()));
        assertThrows(UnsupportedOperationException.class, array.values()::clear);
        assertThrows(UnsupportedOperationException.class, object.properties()::clear);
    }

    @Test
    void testCopy_WhenValueIsNull_ShouldReturnNull() {
        assertNull(ModelCopies.<ApiInfo>copy(null));
    }

    @Test
    void testBuild_WhenChildrenAreAlreadyBuilt_ShouldCopyChildrenIndependently() {
        Components components = ModelFixture.document().getComponents();
        ApiDocument first = ApiDocument.builder(ElementId.random()).components(components).build();
        ApiDocument second = ApiDocument.builder(ElementId.random()).components(components).build();

        assertNotSame(components, first.getComponents());
        ModelAssertions.assertIndependentCopy(components, first.getComponents());
        ModelAssertions.assertIndependentCopy(first.getComponents(), second.getComponents());
    }

    private static Stream<Arguments> builders() {
        return Stream.of(
                builder("document", ApiDocument.builder(ElementId.random())
                        .info(ApiInfo.builder().title("API").build())::build),
                builder("components", Components.builder().schemas(List.of(
                        new SchemaComponent(ElementId.random(), "User", ModelFixture.schema())))::build),
                builder("info", ApiInfo.builder().title("API")::build),
                builder("schema", SchemaObject.builder(ElementId.random()).properties(List.of(
                        new SchemaProperty(ElementId.random(), "name", ModelFixture.schema())))::build),
                builder("discriminator", Discriminator.builder().propertyName("kind")::build),
                builder("path", PathItem.builder(ElementId.random(), "/users").operations(
                        Map.of(HttpMethod.GET, Operation.builder(ElementId.random()).build()))::build),
                builder("operation", Operation.builder(ElementId.random()).parameters(
                        List.of(new InlineObject<>(ModelFixture.parameter())))::build),
                builder("parameter", Parameter.builder(ElementId.random()).schema(ModelFixture.schema())::build),
                builder("header", Header.builder(ElementId.random()).schema(ModelFixture.schema())::build),
                builder("request body", RequestBody.builder(ElementId.random()).content(
                        Map.of("application/json", ModelFixture.mediaType()))::build),
                builder("response", ApiResponse.builder(ElementId.random()).content(
                        Map.of("application/json", ModelFixture.mediaType()))::build),
                builder("media type", MediaType.builder(ElementId.random()).schema(ModelFixture.schema())::build),
                builder("encoding", Encoding.builder(ElementId.random()).headers(
                        Map.of("X-Trace", new InlineObject<>(ModelFixture.header())))::build),
                builder("example", Example.builder(ElementId.random()).value(ModelFixture.value())::build),
                builder("server", Server.builder(ElementId.random()).variables(Map.of("environment",
                        ServerVariable.builder().defaultValue("dev").build()))::build),
                builder("server variable", ServerVariable.builder().enumValues(List.of("dev", "prod"))::build),
                builder("tag", Tag.builder(ElementId.random(), "users")::build),
                builder("security scheme", SecurityScheme.builder(ElementId.random()).flows(
                        OAuthFlows.builder().implicit(OAuthFlow.builder().tokenUrl("https://example.test/token").build()).build())::build),
                builder("security requirement", SecurityRequirement.builder()
                        .schemes(Map.of("oauth", List.of("read")))::build),
                builder("OAuth flows", OAuthFlows.builder().authorizationCode(
                        OAuthFlow.builder().authorizationUrl("https://example.test/authorize").build())::build),
                builder("OAuth flow", OAuthFlow.builder().scopes(Map.of("read", "Read users"))::build));
    }

    private static Arguments builder(String name, Supplier<? extends CopyableModel<?>> build) {
        return Arguments.of(name, build);
    }
}
