package io.github.yluttsev.matte.core.model.path;

import io.github.yluttsev.matte.core.model.ElementId;
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

public final class Operation {

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
}
