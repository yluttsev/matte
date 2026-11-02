package io.github.yluttsev.matte.core.model.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OAuthFlow {

    private String authorizationUrl;
    private String tokenUrl;
    private String refreshUrl;
    private final Map<String, String> scopes = new LinkedHashMap<>();

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
}
