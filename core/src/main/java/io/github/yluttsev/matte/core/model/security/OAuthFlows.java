package io.github.yluttsev.matte.core.model.security;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;

public final class OAuthFlows implements CopyableModel<OAuthFlows> {

    private OAuthFlow implicit;
    private OAuthFlow password;
    private OAuthFlow clientCredentials;
    private OAuthFlow authorizationCode;

    public OAuthFlows() {
    }

    private OAuthFlows(OAuthFlows source) {
        implicit = ModelCopies.copy(source.implicit);
        password = ModelCopies.copy(source.password);
        clientCredentials = ModelCopies.copy(source.clientCredentials);
        authorizationCode = ModelCopies.copy(source.authorizationCode);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public OAuthFlows copy() {
        return new OAuthFlows(this);
    }

    public OAuthFlow getImplicit() {
        return implicit;
    }

    public OAuthFlow getPassword() {
        return password;
    }

    public OAuthFlow getClientCredentials() {
        return clientCredentials;
    }

    public OAuthFlow getAuthorizationCode() {
        return authorizationCode;
    }

    public static final class Builder {

        private final OAuthFlows draft = new OAuthFlows();

        private Builder() {
        }

        public Builder implicit(OAuthFlow implicit) {
            draft.implicit = implicit;
            return this;
        }

        public Builder password(OAuthFlow password) {
            draft.password = password;
            return this;
        }

        public Builder clientCredentials(OAuthFlow clientCredentials) {
            draft.clientCredentials = clientCredentials;
            return this;
        }

        public Builder authorizationCode(OAuthFlow authorizationCode) {
            draft.authorizationCode = authorizationCode;
            return this;
        }

        public OAuthFlows build() {
            return draft.copy();
        }
    }
}
