package io.github.yluttsev.matte.core.model.parameter;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Parameter {

    private final ElementId id;
    private String name;
    private ParameterLocation location;
    private String description;
    private Boolean required;
    private Boolean deprecated;
    private Boolean allowEmptyValue;
    private ParameterStyle style;
    private Boolean explode;
    private Boolean allowReserved;
    private Schema schema;
    private DocumentValue example;
    private final Map<String, ReferenceOr<Example>> examples = new LinkedHashMap<>();
    private final Map<String, MediaType> content = new LinkedHashMap<>();

    public Parameter(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public ElementId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ParameterLocation getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getRequired() {
        return required;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public Boolean getAllowEmptyValue() {
        return allowEmptyValue;
    }

    public ParameterStyle getStyle() {
        return style;
    }

    public Boolean getExplode() {
        return explode;
    }

    public Boolean getAllowReserved() {
        return allowReserved;
    }

    public Schema getSchema() {
        return schema;
    }

    public DocumentValue getExample() {
        return example;
    }

    public Map<String, ReferenceOr<Example>> getExamples() {
        return Collections.unmodifiableMap(examples);
    }

    public Map<String, MediaType> getContent() {
        return Collections.unmodifiableMap(content);
    }
}
