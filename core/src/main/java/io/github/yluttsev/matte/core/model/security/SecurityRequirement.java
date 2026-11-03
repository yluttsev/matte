package io.github.yluttsev.matte.core.model.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;

public final class SecurityRequirement implements CopyableModel<SecurityRequirement> {

    private final Map<String, List<String>> schemes = new LinkedHashMap<>();

    public SecurityRequirement() {
    }

    private SecurityRequirement(SecurityRequirement source) {
        source.schemes.forEach((name, scopes) -> schemes.put(name, List.copyOf(scopes)));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SecurityRequirement copy() {
        return new SecurityRequirement(this);
    }

    public Map<String, List<String>> getSchemes() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        schemes.forEach((name, scopes) -> result.put(name, List.copyOf(scopes)));
        return Collections.unmodifiableMap(result);
    }

    public static final class Builder {

        private final SecurityRequirement draft = new SecurityRequirement();

        private Builder() {
        }

        public Builder schemes(Map<String, List<String>> schemes) {
            Map<String, List<String>> values = new LinkedHashMap<>();
            schemes.forEach((name, scopes) -> values.put(
                    Objects.requireNonNull(name, "scheme name"), List.copyOf(scopes)));
            draft.schemes.clear();
            draft.schemes.putAll(values);
            return this;
        }

        public SecurityRequirement build() {
            return draft.copy();
        }
    }
}
