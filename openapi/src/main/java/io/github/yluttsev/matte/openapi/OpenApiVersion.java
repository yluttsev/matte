package io.github.yluttsev.matte.openapi;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public enum OpenApiVersion {
    V3_0("3\\.0\\.[0-9]+"),
    V3_1("3\\.1\\.[0-9]+");

    private final Pattern pattern;

    OpenApiVersion(String expression) {
        pattern = Pattern.compile(expression);
    }

    public static Optional<OpenApiVersion> supported(String value) {
        Objects.requireNonNull(value, "value");
        return Arrays.stream(values())
                .filter(version -> version.pattern.matcher(value).matches())
                .findFirst();
    }
}
