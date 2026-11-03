package io.github.yluttsev.matte.core.model.parameter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;
import io.github.yluttsev.matte.core.editing.MutationAccess;
import io.github.yluttsev.matte.core.editing.ReferenceOwner;
import io.github.yluttsev.matte.core.editing.ReferenceUpdates;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

public final class Parameter implements IdentifiedElement, CopyableModel<Parameter>, ReferenceOwner {

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

    private Parameter(Parameter source) {
        this(source.id);
        name = source.name;
        location = source.location;
        description = source.description;
        required = source.required;
        deprecated = source.deprecated;
        allowEmptyValue = source.allowEmptyValue;
        style = source.style;
        explode = source.explode;
        allowReserved = source.allowReserved;
        schema = ModelCopies.copy(source.schema);
        example = source.example;
        examples.putAll(ModelCopies.map(source.examples));
        content.putAll(ModelCopies.map(source.content));
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public Parameter copy() {
        return new Parameter(this);
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

    @Override
    public void rewriteReferences(MutationAccess access, UnaryOperator<String> transform) {
        access.require(this);
        ReferenceUpdates.map(examples, transform);
    }

    public static final class Builder {

        private final Parameter draft;

        private Builder(ElementId id) {
            draft = new Parameter(id);
        }

        public Builder name(String name) {
            draft.name = name;
            return this;
        }

        public Builder location(ParameterLocation location) {
            draft.location = location;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder required(Boolean required) {
            draft.required = required;
            return this;
        }

        public Builder deprecated(Boolean deprecated) {
            draft.deprecated = deprecated;
            return this;
        }

        public Builder allowEmptyValue(Boolean allowEmptyValue) {
            draft.allowEmptyValue = allowEmptyValue;
            return this;
        }

        public Builder style(ParameterStyle style) {
            draft.style = style;
            return this;
        }

        public Builder explode(Boolean explode) {
            draft.explode = explode;
            return this;
        }

        public Builder allowReserved(Boolean allowReserved) {
            draft.allowReserved = allowReserved;
            return this;
        }

        public Builder schema(Schema schema) {
            draft.schema = schema;
            return this;
        }

        public Builder example(DocumentValue example) {
            draft.example = example;
            return this;
        }

        public Builder examples(Map<String, ReferenceOr<Example>> examples) {
            Map<String, ReferenceOr<Example>> values = ModelCopies.valueMap(examples);
            draft.examples.clear();
            draft.examples.putAll(values);
            return this;
        }

        public Builder content(Map<String, MediaType> content) {
            Map<String, MediaType> values = ModelCopies.valueMap(content);
            draft.content.clear();
            draft.content.putAll(values);
            return this;
        }

        public Parameter build() {
            return draft.copy();
        }
    }
}
