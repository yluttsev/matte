package io.github.yluttsev.matte.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;
import io.github.yluttsev.matte.core.editing.MutationAccess;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;

public final class Components implements CopyableModel<Components> {

    private final List<SchemaComponent> schemas = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<Parameter>>> parameters = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<RequestBody>>> requestBodies = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<ApiResponse>>> responses = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<Header>>> headers = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<SecurityScheme>>> securitySchemes = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<Example>>> examples = new ArrayList<>();

    public Components() {
    }

    private Components(Components source) {
        schemas.addAll(ModelCopies.list(source.schemas));
        parameters.addAll(ModelCopies.list(source.parameters));
        requestBodies.addAll(ModelCopies.list(source.requestBodies));
        responses.addAll(ModelCopies.list(source.responses));
        headers.addAll(ModelCopies.list(source.headers));
        securitySchemes.addAll(ModelCopies.list(source.securitySchemes));
        examples.addAll(ModelCopies.list(source.examples));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Components copy() {
        return new Components(this);
    }

    public void addSchemaComponent(MutationAccess access, SchemaComponent component) {
        access.require(this);
        schemas.add(Objects.requireNonNull(component, "component").copy());
    }

    public void removeSchemaComponent(MutationAccess access, SchemaComponent component) {
        access.require(this);
        schemas.remove(Objects.requireNonNull(component, "component"));
    }

    public List<SchemaComponent> getSchemas() {
        return Collections.unmodifiableList(schemas);
    }

    public List<NamedComponent<ReferenceOr<Parameter>>> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public List<NamedComponent<ReferenceOr<RequestBody>>> getRequestBodies() {
        return Collections.unmodifiableList(requestBodies);
    }

    public List<NamedComponent<ReferenceOr<ApiResponse>>> getResponses() {
        return Collections.unmodifiableList(responses);
    }

    public List<NamedComponent<ReferenceOr<Header>>> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public List<NamedComponent<ReferenceOr<SecurityScheme>>> getSecuritySchemes() {
        return Collections.unmodifiableList(securitySchemes);
    }

    public List<NamedComponent<ReferenceOr<Example>>> getExamples() {
        return Collections.unmodifiableList(examples);
    }

    public static final class Builder {

        private final Components draft = new Components();

        private Builder() {
        }

        public Builder schemas(List<SchemaComponent> schemas) {
            List<SchemaComponent> values = List.copyOf(schemas);
            draft.schemas.clear();
            draft.schemas.addAll(values);
            return this;
        }

        public Builder parameters(List<NamedComponent<ReferenceOr<Parameter>>> parameters) {
            List<NamedComponent<ReferenceOr<Parameter>>> values = List.copyOf(parameters);
            draft.parameters.clear();
            draft.parameters.addAll(values);
            return this;
        }

        public Builder requestBodies(List<NamedComponent<ReferenceOr<RequestBody>>> requestBodies) {
            List<NamedComponent<ReferenceOr<RequestBody>>> values = List.copyOf(requestBodies);
            draft.requestBodies.clear();
            draft.requestBodies.addAll(values);
            return this;
        }

        public Builder responses(List<NamedComponent<ReferenceOr<ApiResponse>>> responses) {
            List<NamedComponent<ReferenceOr<ApiResponse>>> values = List.copyOf(responses);
            draft.responses.clear();
            draft.responses.addAll(values);
            return this;
        }

        public Builder headers(List<NamedComponent<ReferenceOr<Header>>> headers) {
            List<NamedComponent<ReferenceOr<Header>>> values = List.copyOf(headers);
            draft.headers.clear();
            draft.headers.addAll(values);
            return this;
        }

        public Builder securitySchemes(List<NamedComponent<ReferenceOr<SecurityScheme>>> securitySchemes) {
            List<NamedComponent<ReferenceOr<SecurityScheme>>> values = List.copyOf(securitySchemes);
            draft.securitySchemes.clear();
            draft.securitySchemes.addAll(values);
            return this;
        }

        public Builder examples(List<NamedComponent<ReferenceOr<Example>>> examples) {
            List<NamedComponent<ReferenceOr<Example>>> values = List.copyOf(examples);
            draft.examples.clear();
            draft.examples.addAll(values);
            return this;
        }

        public Components build() {
            return draft.copy();
        }
    }
}
