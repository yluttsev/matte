package io.github.yluttsev.matte.core.construction.traversal;

import java.util.function.BiConsumer;
import io.github.yluttsev.matte.core.model.security.OAuthFlow;
import io.github.yluttsev.matte.core.model.security.OAuthFlows;
import io.github.yluttsev.matte.core.model.security.SecurityRequirement;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;

final class SecurityChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, BiConsumer<Object, String> visitor) {
        if (node instanceof SecurityScheme scheme) {
            visitor.accept(scheme.getFlows(), "flows");
        } else if (node instanceof OAuthFlows flows) {
            visitor.accept(flows.getImplicit(), "implicit");
            visitor.accept(flows.getPassword(), "password");
            visitor.accept(flows.getClientCredentials(), "clientCredentials");
            visitor.accept(flows.getAuthorizationCode(), "authorizationCode");
        } else {
            return isLeafModel(node);
        }
        return true;
    }

    private static boolean isLeafModel(Object node) {
        return node instanceof SecurityRequirement || node instanceof OAuthFlow;
    }
}
