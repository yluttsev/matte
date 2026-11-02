package io.github.yluttsev.matte.core.model.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServerVariable {

    private String defaultValue;
    private String description;
    private final List<String> enumValues = new ArrayList<>();

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEnumValues() {
        return Collections.unmodifiableList(enumValues);
    }
}
