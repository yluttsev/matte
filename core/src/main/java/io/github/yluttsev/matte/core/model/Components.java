package io.github.yluttsev.matte.core.model;

import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Components {

    private final List<SchemaComponent> schemas = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<Parameter>>> parameters = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<RequestBody>>> requestBodies = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<ApiResponse>>> responses = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<Header>>> headers = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<SecurityScheme>>> securitySchemes = new ArrayList<>();
    private final List<NamedComponent<ReferenceOr<Example>>> examples = new ArrayList<>();

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
}
