package io.github.yluttsev.matte.core.model.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;

public final class OAuthFlow implements CopyableModel<OAuthFlow> {

    private String authorizationUrl;
    private String tokenUrl;
    private String refreshUrl;
    private final Map<String, String> scopes = new LinkedHashMap<>();

    public OAuthFlow() {
    }

    private OAuthFlow(OAuthFlow source) {
        authorizationUrl = source.authorizationUrl;
        tokenUrl = source.tokenUrl;
        refreshUrl = source.refreshUrl;
        scopes.putAll(ModelCopies.copyMapEntries(source.scopes));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public OAuthFlow copy() {
        return new OAuthFlow(this);
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public Map<String, String> getScopes() {
        return Collections.unmodifiableMap(scopes);
    }

    public static final class Builder {

        private final OAuthFlow draft = new OAuthFlow();

        private Builder() {
        }

        public Builder authorizationUrl(String authorizationUrl) {
            draft.authorizationUrl = authorizationUrl;
            return this;
        }

        public Builder tokenUrl(String tokenUrl) {
            draft.tokenUrl = tokenUrl;
            return this;
        }

        public Builder refreshUrl(String refreshUrl) {
            draft.refreshUrl = refreshUrl;
            return this;
        }

        public Builder scopes(Map<String, String> scopes) {
            Map<String, String> values = ModelCopies.copyMapEntries(scopes);
            draft.scopes.clear();
            draft.scopes.putAll(values);
            return this;
        }

        public OAuthFlow build() {
            return draft.copy();
        }
    }
}
