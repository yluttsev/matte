package io.github.yluttsev.matte.core.construction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.NamedComponent;
import io.github.yluttsev.matte.core.model.info.ApiInfo;
import io.github.yluttsev.matte.core.model.info.Contact;
import io.github.yluttsev.matte.core.model.info.ExternalDocumentation;
import io.github.yluttsev.matte.core.model.info.License;
import io.github.yluttsev.matte.core.model.media.Encoding;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.parameter.ParameterLocation;
import io.github.yluttsev.matte.core.model.parameter.ParameterStyle;
import io.github.yluttsev.matte.core.model.path.HttpMethod;
import io.github.yluttsev.matte.core.model.path.Operation;
import io.github.yluttsev.matte.core.model.path.PathItem;
import io.github.yluttsev.matte.core.model.reference.InlineObject;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.response.ResponseKey;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.JsonType;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.security.ApiKeyLocation;
import io.github.yluttsev.matte.core.model.security.OAuthFlow;
import io.github.yluttsev.matte.core.model.security.OAuthFlows;
import io.github.yluttsev.matte.core.model.security.SecurityRequirement;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;
import io.github.yluttsev.matte.core.model.security.SecuritySchemeType;
import io.github.yluttsev.matte.core.model.server.Server;
import io.github.yluttsev.matte.core.model.server.ServerVariable;
import io.github.yluttsev.matte.core.model.tag.Tag;
import io.github.yluttsev.matte.core.model.value.ArrayValue;
import io.github.yluttsev.matte.core.model.value.BooleanValue;
import io.github.yluttsev.matte.core.model.value.DocumentValue;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.NumberValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;

public final class ModelFixture {

    private ModelFixture() {
    }

    public static ApiDocument document() {
        Operation operation = Operation.builder(ElementId.random())
                .operationId("getUsers")
                .summary("Get users")
                .description("Returns users")
                .deprecated(true)
                .externalDocs(documentation())
                .tags(List.of("users", "public"))
                .parameters(List.of(new InlineObject<>(parameter()), reference("#/components/parameters/Limit")))
                .requestBody(new InlineObject<>(requestBody()))
                .responses(responses())
                .servers(List.of(server()))
                .security(List.of(requirement()))
                .build();
        PathItem path = PathItem.builder(ElementId.random(), "/users")
                .summary("Users")
                .description("Users resource")
                .operations(Map.of(HttpMethod.GET, operation))
                .parameters(List.of(new InlineObject<>(parameter())))
                .servers(List.of(server()))
                .build();
        Components components = Components.builder()
                .schemas(List.of(new SchemaComponent(ElementId.random(), "User", schema())))
                .parameters(List.of(component("Limit", parameter())))
                .requestBodies(List.of(component("Input", requestBody())))
                .responses(List.of(component("Success", response())))
                .headers(List.of(component("Trace", header())))
                .securitySchemes(List.of(component("OAuth", securityScheme())))
                .examples(List.of(component("UserExample", example())))
                .build();
        return ApiDocument.builder(ElementId.random())
                .info(ApiInfo.builder()
                        .title("Users API")
                        .summary("API summary")
                        .description("API description")
                        .version("1.2")
                        .termsOfService("https://example.test/terms")
                        .contact(new Contact("Support", "https://example.test", "support@example.test"))
                        .license(new License("Apache 2.0", "Apache-2.0", "https://example.test/license"))
                        .build())
                .components(components)
                .paths(List.of(path))
                .servers(List.of(server()))
                .tags(List.of(Tag.builder(ElementId.random(), "users")
                        .description("User operations")
                        .externalDocs(documentation())
                        .build()))
                .security(List.of(requirement()))
                .externalDocs(documentation())
                .build();
    }

    public static SchemaObject schema() {
        return SchemaObject.builder(ElementId.random())
                .types(Set.of(JsonType.OBJECT, JsonType.NULL))
                .format("custom")
                .title("User schema")
                .description("Schema description")
                .reference(new SchemaReference("#/components/schemas/Base"))
                .defaultValue(new NullValue())
                .example(value())
                .examples(List.of(value(), new StringValue("other")))
                .enumValues(List.of(new NumberValue(new BigDecimal("1.25")), new NullValue()))
                .constValue(new BooleanValue(true))
                .deprecated(true)
                .readOnly(true)
                .writeOnly(true)
                .properties(List.of(new SchemaProperty(ElementId.random(), "name", scalar())))
                .required(Set.of("name", "notYetDefined"))
                .additionalProperties(new BooleanSchema(ElementId.random(), false))
                .minProperties(BigInteger.ONE)
                .maxProperties(BigInteger.TEN)
                .items(scalar())
                .minItems(BigInteger.TWO)
                .maxItems(BigInteger.valueOf(20))
                .uniqueItems(true)
                .minLength(BigInteger.valueOf(3))
                .maxLength(BigInteger.valueOf(30))
                .pattern("[a-z]+")
                .minimum(new BigDecimal("1.1"))
                .maximum(new BigDecimal("10.1"))
                .exclusiveMinimum(new BigDecimal("0.1"))
                .exclusiveMaximum(new BigDecimal("11.1"))
                .multipleOf(new BigDecimal("0.5"))
                .allOf(List.of(scalar()))
                .oneOf(List.of(new BooleanSchema(ElementId.random(), true)))
                .anyOf(List.of(scalar()))
                .not(new BooleanSchema(ElementId.random(), false))
                .discriminator(Discriminator.builder()
                        .propertyName("kind")
                        .mapping(Map.of("user", new SchemaReference("#/components/schemas/User")))
                        .build())
                .build();
    }

    public static Parameter parameter() {
        return Parameter.builder(ElementId.random())
                .name("limit")
                .location(ParameterLocation.QUERY)
                .description("Page limit")
                .required(true)
                .deprecated(true)
                .allowEmptyValue(true)
                .style(ParameterStyle.FORM)
                .explode(true)
                .allowReserved(true)
                .schema(scalar())
                .example(value())
                .examples(Map.of("sample", new InlineObject<>(example())))
                .content(Map.of("application/json", mediaType()))
                .build();
    }

    public static Header header() {
        return Header.builder(ElementId.random())
                .description("Trace header")
                .required(true)
                .deprecated(true)
                .style(ParameterStyle.SIMPLE)
                .explode(true)
                .schema(scalar())
                .example(new StringValue("trace"))
                .examples(Map.of("trace", new InlineObject<>(example())))
                .content(Map.of("text/plain", MediaType.builder(ElementId.random()).schema(scalar()).build()))
                .build();
    }

    public static MediaType mediaType() {
        Encoding encoding = Encoding.builder(ElementId.random())
                .contentType("text/plain")
                .headers(Map.of("X-Trace", new InlineObject<>(header())))
                .style(ParameterStyle.FORM)
                .explode(true)
                .allowReserved(true)
                .build();
        return MediaType.builder(ElementId.random())
                .schema(SchemaObject.builder(ElementId.random())
                        .reference(new SchemaReference("#/components/schemas/User"))
                        .build())
                .example(value())
                .examples(Map.of("sample", new InlineObject<>(example())))
                .encoding(Map.of("name", encoding))
                .build();
    }

    public static RequestBody requestBody() {
        return RequestBody.builder(ElementId.random())
                .description("Input body")
                .required(true)
                .content(Map.of("application/json", mediaType()))
                .build();
    }

    public static ApiResponse response() {
        return ApiResponse.builder(ElementId.random())
                .description("Successful response")
                .headers(Map.of("X-Trace", new InlineObject<>(header())))
                .content(Map.of("application/json", mediaType()))
                .build();
    }

    public static Example example() {
        return Example.builder(ElementId.random())
                .summary("Sample")
                .description("Sample description")
                .value(value())
                .externalValue("https://example.test/sample.json")
                .build();
    }

    public static Server server() {
        return Server.builder(ElementId.random())
                .url("https://{environment}.example.test")
                .description("API server")
                .variables(Map.of("environment", ServerVariable.builder()
                        .defaultValue("dev")
                        .description("Environment")
                        .enumValues(List.of("dev", "prod"))
                        .build()))
                .build();
    }

    public static SecurityScheme securityScheme() {
        return SecurityScheme.builder(ElementId.random())
                .type(SecuritySchemeType.OAUTH2)
                .description("OAuth scheme")
                .name("Authorization")
                .location(ApiKeyLocation.HEADER)
                .scheme("bearer")
                .bearerFormat("JWT")
                .flows(OAuthFlows.builder()
                        .implicit(flow())
                        .password(flow())
                        .clientCredentials(flow())
                        .authorizationCode(flow())
                        .build())
                .openIdConnectUrl("https://example.test/.well-known/openid-configuration")
                .build();
    }

    public static DocumentValue value() {
        Map<String, DocumentValue> properties = new LinkedHashMap<>();
        properties.put("name", new StringValue("Ada"));
        properties.put("values", new ArrayValue(List.of(
                new NumberValue(new BigDecimal("42.50")), new BooleanValue(true), new NullValue())));
        return new ObjectValue(properties);
    }

    public static <T extends CopyableModel<T>> NamedComponent<ReferenceOr<T>> component(String name, T value) {
        return new NamedComponent<>(ElementId.random(), name, new InlineObject<>(value));
    }

    public static <T extends CopyableModel<T>> NamedComponent<ReferenceOr<T>> referenceComponent(String name) {
        return new NamedComponent<>(ElementId.random(), name, ModelFixture.<T>reference("#/missing"));
    }

    public static <T extends CopyableModel<T>> ObjectReference<T> reference(String value) {
        return new ObjectReference<>(ElementId.random(), value, "Reference summary", "Reference description");
    }

    private static SchemaObject scalar() {
        return SchemaObject.builder(ElementId.random()).types(Set.of(JsonType.STRING)).build();
    }

    private static ExternalDocumentation documentation() {
        return new ExternalDocumentation("API documentation", "https://example.test/docs");
    }

    private static SecurityRequirement requirement() {
        return SecurityRequirement.builder().schemes(Map.of("OAuth", List.of("read", "write"))).build();
    }

    private static OAuthFlow flow() {
        return OAuthFlow.builder()
                .authorizationUrl("https://example.test/authorize")
                .tokenUrl("https://example.test/token")
                .refreshUrl("https://example.test/refresh")
                .scopes(Map.of("read", "Read users", "write", "Write users"))
                .build();
    }

    private static Map<ResponseKey, ReferenceOr<ApiResponse>> responses() {
        Map<ResponseKey, ReferenceOr<ApiResponse>> responses = new LinkedHashMap<>();
        responses.put(new ResponseKey("200"), new InlineObject<>(response()));
        responses.put(new ResponseKey("2XX"), reference("#/components/responses/Success"));
        responses.put(new ResponseKey("default"), reference("https://example.test/responses.yaml#/Error"));
        return responses;
    }
}
