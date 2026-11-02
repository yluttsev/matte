package io.github.yluttsev.matte.core.model.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Discriminator {

    private String propertyName;
    private final Map<String, SchemaReference> mapping = new LinkedHashMap<>();

    public String getPropertyName() {
        return propertyName;
    }

    public Map<String, SchemaReference> getMapping() {
        return Collections.unmodifiableMap(mapping);
    }
}
