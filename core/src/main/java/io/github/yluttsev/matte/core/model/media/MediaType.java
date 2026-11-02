package io.github.yluttsev.matte.core.model.media;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MediaType {

    private final ElementId id;
    private Schema schema;
    private DocumentValue example;
    private final Map<String, ReferenceOr<Example>> examples = new LinkedHashMap<>();
    private final Map<String, Encoding> encoding = new LinkedHashMap<>();

    public MediaType(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public ElementId getId() {
        return id;
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

    public Map<String, Encoding> getEncoding() {
        return Collections.unmodifiableMap(encoding);
    }
}
