package io.github.yluttsev.matte.core.model.media;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.ParameterStyle;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Encoding {

    private final ElementId id;
    private String contentType;
    private final Map<String, ReferenceOr<Header>> headers = new LinkedHashMap<>();
    private ParameterStyle style;
    private Boolean explode;
    private Boolean allowReserved;

    public Encoding(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public ElementId getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, ReferenceOr<Header>> getHeaders() {
        return Collections.unmodifiableMap(headers);
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
}
