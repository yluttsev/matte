package io.github.yluttsev.matte.core.model.security;

public final class OAuthFlows {

    private OAuthFlow implicit;
    private OAuthFlow password;
    private OAuthFlow clientCredentials;
    private OAuthFlow authorizationCode;

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
}
