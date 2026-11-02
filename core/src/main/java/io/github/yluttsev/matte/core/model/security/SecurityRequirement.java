package io.github.yluttsev.matte.core.model.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Schemes in one requirement are combined with AND; requirement entries use OR. */
public final class SecurityRequirement {

    private final Map<String, List<String>> schemes = new LinkedHashMap<>();

    public Map<String, List<String>> getSchemes() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        schemes.forEach((name, scopes) -> result.put(name, List.copyOf(scopes)));
        return Collections.unmodifiableMap(result);
    }
}
