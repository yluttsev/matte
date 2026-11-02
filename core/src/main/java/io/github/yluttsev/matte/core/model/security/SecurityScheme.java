package io.github.yluttsev.matte.core.model.security;

import io.github.yluttsev.matte.core.model.ElementId;

import java.util.Objects;

public final class SecurityScheme {

    private final ElementId id;
    private SecuritySchemeType type;
    private String description;
    private String name;
    private ApiKeyLocation location;
    private String scheme;
    private String bearerFormat;
    private OAuthFlows flows;
    private String openIdConnectUrl;

    public SecurityScheme(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public ElementId getId() {
        return id;
    }

    public SecuritySchemeType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public ApiKeyLocation getLocation() {
        return location;
    }

    public String getScheme() {
        return scheme;
    }

    public String getBearerFormat() {
        return bearerFormat;
    }

    public OAuthFlows getFlows() {
        return flows;
    }

    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }
}
