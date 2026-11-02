package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.model.security.OAuthFlow;
import io.github.yluttsev.matte.core.model.security.OAuthFlows;
import io.github.yluttsev.matte.core.model.security.SecurityRequirement;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;
import java.util.function.BiConsumer;

final class SecurityChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, String path, BiConsumer<Object, String> visitor) {
        if (node instanceof SecurityScheme scheme) {
            visitor.accept(scheme.getFlows(), path + ".flows");
        } else if (node instanceof OAuthFlows flows) {
            visitor.accept(flows.getImplicit(), path + ".implicit");
            visitor.accept(flows.getPassword(), path + ".password");
            visitor.accept(flows.getClientCredentials(), path + ".clientCredentials");
            visitor.accept(flows.getAuthorizationCode(), path + ".authorizationCode");
        } else {
            return node instanceof SecurityRequirement || node instanceof OAuthFlow;
        }
        return true;
    }
}
