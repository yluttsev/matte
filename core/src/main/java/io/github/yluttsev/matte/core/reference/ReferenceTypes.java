package io.github.yluttsev.matte.core.reference;

import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ReferenceTypes {

    private static final Map<String, Class<? extends CopyableModel<?>>> COLLECTION_TYPES = Map.of(
            "parameters", Parameter.class,
            "requestBodies", RequestBody.class,
            "responses", ApiResponse.class,
            "headers", Header.class,
            "securitySchemes", SecurityScheme.class,
            "examples", Example.class);

    private ReferenceTypes() {
    }

    static Optional<Class<? extends CopyableModel<?>>> at(DocumentPath path) {
        List<String> segments = path.segments();
        if (segments.size() >= 2) {
            Class<? extends CopyableModel<?>> type = COLLECTION_TYPES.get(segments.get(segments.size() - 2));
            if (type != null) {
                return Optional.of(type);
            }
        }
        return !segments.isEmpty() && segments.get(segments.size() - 1).equals("requestBody")
                ? Optional.of(RequestBody.class) : Optional.empty();
    }
}
