package io.github.yluttsev.matte.core.reference;

import java.util.List;
import java.util.Map;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.NamedComponent;
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
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.response.ResponseKey;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.model.schema.SchemaReference;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

final class ReferenceFixture {

    private ReferenceFixture() {
    }

    static DocumentIndex index() {
        SchemaObject user = SchemaObject.builder(ElementId.random())
                .properties(List.of(new SchemaProperty(ElementId.random(), "name",
                        new BooleanSchema(ElementId.random(), true))))
                .example(new ObjectValue(Map.of("$ref", new StringValue("#/not-a-reference"))))
                .build();
        SchemaObject derived = SchemaObject.builder(ElementId.random())
                .allOf(List.of(schemaReference("#/components/schemas/User")))
                .discriminator(Discriminator.builder().propertyName("kind").mapping(Map.of(
                        "user", new SchemaReference("User"),
                        "name", new SchemaReference("#/components/schemas/User/properties/name"))).build())
                .build();
        Parameter limit = Parameter.builder(ElementId.random()).name("limit")
                .schema(schemaReference("#/components/schemas/User")).build();
        MediaType mediaType = MediaType.builder(ElementId.random())
                .schema(schemaReference("#/components/schemas/User/properties/name"))
                .examples(Map.of("sample", reference("#/components/examples/Example")))
                .encoding(Map.of("name", Encoding.builder(ElementId.random())
                        .headers(Map.of("X-Trace", reference("#/components/headers/Header"))).build()))
                .build();
        ApiResponse success = ApiResponse.builder(ElementId.random())
                .headers(Map.of("X-Trace", reference("#/components/headers/Header")))
                .content(Map.of("application/json", mediaType)).build();
        Components components = Components.builder()
                .schemas(List.of(
                        new SchemaComponent(ElementId.random(), "User", user),
                        new SchemaComponent(ElementId.random(), "User2", new BooleanSchema(ElementId.random(), false)),
                        new SchemaComponent(ElementId.random(), "Self", schemaReference("#/components/schemas/Self")),
                        new SchemaComponent(ElementId.random(), "Derived", derived)))
                .parameters(List.of(
                        component("Limit", limit), alias("Alias", "#/components/parameters/Limit"),
                        alias("CycleA", "#/components/parameters/CycleB"),
                        alias("CycleB", "#/components/parameters/CycleA"),
                        alias("BrokenAlias", "#/components/parameters/Missing"),
                        alias("ExternalAlias", "other.yaml#/components/parameters/Limit")))
                .requestBodies(List.of(component("Body", new RequestBody(ElementId.random())),
                        alias("AliasBody", "#/components/requestBodies/Body")))
                .responses(List.of(
                        component("Success", success),
                        alias("AliasResponse", "#/components/responses/Success")))
                .headers(List.of(component("Header", new Header(ElementId.random())),
                        alias("AliasHeader", "#/components/headers/Header")))
                .examples(List.of(component("Example", new Example(ElementId.random())),
                        alias("AliasExample", "#/components/examples/Example")))
                .securitySchemes(List.of(component("Key", new SecurityScheme(ElementId.random())),
                        alias("AliasKey", "#/components/securitySchemes/Key")))
                .build();
        Operation operation = Operation.builder(ElementId.random())
                .parameters(List.of(reference("#/components/parameters/Alias"), new InlineObject<>(
                        Parameter.builder(ElementId.random())
                                .schema(schemaReference("#/components/schemas/User2")).build())))
                .requestBody(reference("#/components/requestBodies/Body"))
                .responses(Map.of(new ResponseKey("200"), reference("#/components/responses/Success")))
                .build();
        ApiDocument document = ApiDocument.builder(ElementId.random()).components(components)
                .paths(List.of(PathItem.builder(ElementId.random(), "/users")
                        .parameters(List.of(reference("#/components/parameters/Limit")))
                        .operations(Map.of(HttpMethod.GET, operation)).build()))
                .build();
        return DocumentIndex.build(document);
    }

    private static SchemaObject schemaReference(String reference) {
        return SchemaObject.builder(ElementId.random()).reference(new SchemaReference(reference)).build();
    }

    private static <T extends CopyableModel<T>> NamedComponent<ReferenceOr<T>> component(String name, T value) {
        return new NamedComponent<>(ElementId.random(), name, new InlineObject<>(value));
    }

    private static <T extends CopyableModel<T>> NamedComponent<ReferenceOr<T>> alias(String name, String value) {
        return new NamedComponent<>(ElementId.random(), name, ReferenceFixture.<T>reference(value));
    }

    private static <T extends CopyableModel<T>> ObjectReference<T> reference(String value) {
        return new ObjectReference<>(ElementId.random(), value, null, null);
    }
}
