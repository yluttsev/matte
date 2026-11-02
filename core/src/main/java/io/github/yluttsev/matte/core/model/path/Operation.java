package io.github.yluttsev.matte.core.model.path;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.info.ExternalDocumentation;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.response.ResponseKey;
import io.github.yluttsev.matte.core.model.security.SecurityRequirement;
import io.github.yluttsev.matte.core.model.server.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Operation implements IdentifiedElement, CopyableModel<Operation> {

    private final ElementId id;
    private String operationId;
    private String summary;
    private String description;
    private Boolean deprecated;
    private ExternalDocumentation externalDocs;
    private final List<String> tags = new ArrayList<>();
    private final List<ReferenceOr<Parameter>> parameters = new ArrayList<>();
    private ReferenceOr<RequestBody> requestBody;
    private final Map<ResponseKey, ReferenceOr<ApiResponse>> responses = new LinkedHashMap<>();
    private final List<Server> servers = new ArrayList<>();
    private boolean securityOverride;
    private final List<SecurityRequirement> security = new ArrayList<>();

    public Operation(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private Operation(Operation source) {
        this(source.id);
        operationId = source.operationId;
        summary = source.summary;
        description = source.description;
        deprecated = source.deprecated;
        externalDocs = source.externalDocs;
        tags.addAll(source.tags);
        parameters.addAll(ModelCopies.list(source.parameters));
        requestBody = ModelCopies.copy(source.requestBody);
        responses.putAll(ModelCopies.map(source.responses));
        servers.addAll(ModelCopies.list(source.servers));
        securityOverride = source.securityOverride;
        security.addAll(ModelCopies.list(source.security));
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public Operation copy() {
        return new Operation(this);
    }

    public ElementId getId() {
        return id;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public List<ReferenceOr<Parameter>> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public ReferenceOr<RequestBody> getRequestBody() {
        return requestBody;
    }

    public Map<ResponseKey, ReferenceOr<ApiResponse>> getResponses() {
        return Collections.unmodifiableMap(responses);
    }

    public List<Server> getServers() {
        return Collections.unmodifiableList(servers);
    }

    public boolean hasSecurityOverride() {
        return securityOverride;
    }

    public List<SecurityRequirement> getSecurity() {
        return Collections.unmodifiableList(security);
    }

    public static final class Builder {

        private final Operation draft;

        private Builder(ElementId id) {
            draft = new Operation(id);
        }

        public Builder operationId(String operationId) {
            draft.operationId = operationId;
            return this;
        }

        public Builder summary(String summary) {
            draft.summary = summary;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder deprecated(Boolean deprecated) {
            draft.deprecated = deprecated;
            return this;
        }

        public Builder externalDocs(ExternalDocumentation externalDocs) {
            draft.externalDocs = externalDocs;
            return this;
        }

        public Builder tags(List<String> tags) {
            List<String> values = List.copyOf(tags);
            draft.tags.clear();
            draft.tags.addAll(values);
            return this;
        }

        public Builder parameters(List<ReferenceOr<Parameter>> parameters) {
            List<ReferenceOr<Parameter>> values = List.copyOf(parameters);
            draft.parameters.clear();
            draft.parameters.addAll(values);
            return this;
        }

        public Builder requestBody(ReferenceOr<RequestBody> requestBody) {
            draft.requestBody = requestBody;
            return this;
        }

        public Builder responses(Map<ResponseKey, ReferenceOr<ApiResponse>> responses) {
            Map<ResponseKey, ReferenceOr<ApiResponse>> values = ModelCopies.valueMap(responses);
            draft.responses.clear();
            draft.responses.putAll(values);
            return this;
        }

        public Builder servers(List<Server> servers) {
            List<Server> values = List.copyOf(servers);
            draft.servers.clear();
            draft.servers.addAll(values);
            return this;
        }

        public Builder security(List<SecurityRequirement> security) {
            List<SecurityRequirement> values = List.copyOf(security);
            draft.security.clear();
            draft.security.addAll(values);
            draft.securityOverride = true;
            return this;
        }

        public Builder inheritSecurity() {
            draft.security.clear();
            draft.securityOverride = false;
            return this;
        }

        public Operation build() {
            return draft.copy();
        }
    }
}
