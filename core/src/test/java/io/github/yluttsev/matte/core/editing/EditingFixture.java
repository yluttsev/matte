package io.github.yluttsev.matte.core.editing;

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

final class EditingFixture {

    private static final String USER = "#/components/schemas/User";

    private EditingFixture() {
    }

    static ApiDocument document() {
        SchemaObject user = SchemaObject.builder(ElementId.random())
                .title("User title")
                .reference(new SchemaReference(USER))
                .properties(List.of(new SchemaProperty(ElementId.random(), "name", schema(USER + "/properties/name"))))
                .example(new ObjectValue(Map.of("$ref", new StringValue(USER))))
                .build();
        SchemaObject holder = SchemaObject.builder(ElementId.random()).reference(new SchemaReference(USER))
                .discriminator(Discriminator.builder().propertyName("kind").mapping(Map.of(
                        "bare", new SchemaReference("User"),
                        "pointer", new SchemaReference(USER + "/properties/name"),
                        "missing", new SchemaReference(USER + "/properties/missing"),
                        "external", new SchemaReference("other.yaml" + USER))).build())
                .build();
        Components components = Components.builder()
                .schemas(List.of(
                        new SchemaComponent(ElementId.random(), "User", user),
                        new SchemaComponent(ElementId.random(), "User2", new BooleanSchema(ElementId.random(), false)),
                        new SchemaComponent(ElementId.random(), "Holder", holder),
                        new SchemaComponent(ElementId.random(), "Other", schema(USER + "2")),
                        new SchemaComponent(ElementId.random(), "External", schema("other.yaml" + USER)),
                        new SchemaComponent(ElementId.random(), "Anchor", schema("#User")),
                        new SchemaComponent(ElementId.random(), "Malformed", schema("#/bad~2"))))
                .parameters(List.of(component("Parameter", parameter()), alias("Alias", USER)))
                .requestBodies(List.of(component("Body", body()), alias("Alias", USER)))
                .responses(List.of(component("Response", response()), alias("Alias", USER)))
                .headers(List.of(component("Header", header()), alias("Alias", USER)))
                .examples(List.of(component("Example", Example.builder(ElementId.random())
                        .value(new ObjectValue(Map.of("$ref", new StringValue(USER)))).build()), alias("Alias", USER)))
                .securitySchemes(List.of(
                        component("Key", new SecurityScheme(ElementId.random())),
                        alias("Alias", USER)))
                .build();
        Operation operation = Operation.builder(ElementId.random())
                .parameters(List.of(reference(USER), new InlineObject<>(parameter())))
                .requestBody(reference(USER))
                .responses(Map.of(new ResponseKey("200"), reference(USER),
                        new ResponseKey("201"), new InlineObject<>(response())))
                .build();
        return ApiDocument.builder(ElementId.random()).components(components)
                .paths(List.of(PathItem.builder(ElementId.random(), "/users")
                        .parameters(List.of(reference(USER), new InlineObject<>(parameter())))
                        .operations(Map.of(HttpMethod.GET, operation)).build()))
                .build();
    }

    private static SchemaObject schema(String reference) {
        return SchemaObject.builder(ElementId.random()).reference(new SchemaReference(reference)).build();
    }

    private static Parameter parameter() {
        return Parameter.builder(ElementId.random()).name("user").schema(schema(USER))
                .examples(Map.of("user", reference(USER))).content(Map.of("application/json", mediaType())).build();
    }

    private static Header header() {
        return Header.builder(ElementId.random()).schema(schema(USER)).examples(Map.of("user", reference(USER)))
                .content(Map.of("application/json", mediaType())).build();
    }

    private static RequestBody body() {
        return RequestBody.builder(ElementId.random()).content(Map.of("application/json", mediaType())).build();
    }

    private static ApiResponse response() {
        return ApiResponse.builder(ElementId.random()).headers(Map.of("X-User", reference(USER)))
                .content(Map.of("application/json", mediaType())).build();
    }

    private static MediaType mediaType() {
        return MediaType.builder(ElementId.random()).schema(schema(USER))
                .examples(Map.of("user", reference(USER)))
                .encoding(Map.of("name", Encoding.builder(ElementId.random())
                        .headers(Map.of("X-User", reference(USER))).build())).build();
    }

    private static <T extends CopyableModel<T>> NamedComponent<ReferenceOr<T>> component(String name, T value) {
        return new NamedComponent<>(ElementId.random(), name, new InlineObject<>(value));
    }

    private static <T extends CopyableModel<T>> NamedComponent<ReferenceOr<T>> alias(String name, String value) {
        return new NamedComponent<>(ElementId.random(), name, EditingFixture.<T>reference(value));
    }

    private static <T extends CopyableModel<T>> ObjectReference<T> reference(String value) {
        return new ObjectReference<>(ElementId.random(), value, "Reference summary", "Reference description");
    }
}
