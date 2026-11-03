package io.github.yluttsev.matte.core.reference;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import io.github.yluttsev.matte.core.model.security.SecurityScheme;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

final class ReferenceTypes {

    private static final String REQUEST_BODY_FIELD = "requestBody";
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
        Optional<Class<? extends CopyableModel<?>>> collectionType = collectionEntryType(segments);
        if (collectionType.isPresent()) {
            return collectionType;
        }
        return isRequestBodyField(segments) ? Optional.of(RequestBody.class) : Optional.empty();
    }

    private static Optional<Class<? extends CopyableModel<?>>> collectionEntryType(List<String> segments) {
        if (segments.size() < 2) {
            return Optional.empty();
        }
        String parentCollection = segments.get(segments.size() - 2);
        return Optional.ofNullable(COLLECTION_TYPES.get(parentCollection));
    }

    private static boolean isRequestBodyField(List<String> segments) {
        return !segments.isEmpty() && segments.get(segments.size() - 1).equals(REQUEST_BODY_FIELD);
    }
}
