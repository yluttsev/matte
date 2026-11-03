package io.github.yluttsev.matte.core.model.security;

import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;

public final class SecurityScheme implements IdentifiedElement, CopyableModel<SecurityScheme> {

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

    private SecurityScheme(SecurityScheme source) {
        this(source.id);
        type = source.type;
        description = source.description;
        name = source.name;
        location = source.location;
        scheme = source.scheme;
        bearerFormat = source.bearerFormat;
        flows = ModelCopies.copy(source.flows);
        openIdConnectUrl = source.openIdConnectUrl;
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public SecurityScheme copy() {
        return new SecurityScheme(this);
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

    public static final class Builder {

        private final SecurityScheme draft;

        private Builder(ElementId id) {
            draft = new SecurityScheme(id);
        }

        public Builder type(SecuritySchemeType type) {
            draft.type = type;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder name(String name) {
            draft.name = name;
            return this;
        }

        public Builder location(ApiKeyLocation location) {
            draft.location = location;
            return this;
        }

        public Builder scheme(String scheme) {
            draft.scheme = scheme;
            return this;
        }

        public Builder bearerFormat(String bearerFormat) {
            draft.bearerFormat = bearerFormat;
            return this;
        }

        public Builder flows(OAuthFlows flows) {
            draft.flows = flows;
            return this;
        }

        public Builder openIdConnectUrl(String openIdConnectUrl) {
            draft.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        public SecurityScheme build() {
            return draft.copy();
        }
    }
}
