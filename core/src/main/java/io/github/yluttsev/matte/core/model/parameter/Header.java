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

public final class Header implements IdentifiedElement, CopyableModel<Header>, ReferenceOwner {

    private final ElementId id;
    private String description;
    private Boolean required;
    private Boolean deprecated;
    private ParameterStyle style;
    private Boolean explode;
    private Schema schema;
    private DocumentValue example;
    private final Map<String, ReferenceOr<Example>> examples = new LinkedHashMap<>();
    private final Map<String, MediaType> content = new LinkedHashMap<>();

    public Header(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private Header(Header source) {
        this(source.id);
        description = source.description;
        required = source.required;
        deprecated = source.deprecated;
        style = source.style;
        explode = source.explode;
        schema = ModelCopies.copy(source.schema);
        example = source.example;
        examples.putAll(ModelCopies.copyModelMap(source.examples));
        content.putAll(ModelCopies.copyModelMap(source.content));
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public Header copy() {
        return new Header(this);
    }

    public ElementId getId() {
        return id;
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

    public ParameterStyle getStyle() {
        return style;
    }

    public Boolean getExplode() {
        return explode;
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

        private final Header draft;

        private Builder(ElementId id) {
            draft = new Header(id);
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

        public Builder style(ParameterStyle style) {
            draft.style = style;
            return this;
        }

        public Builder explode(Boolean explode) {
            draft.explode = explode;
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
            Map<String, ReferenceOr<Example>> values = ModelCopies.copyMapEntries(examples);
            draft.examples.clear();
            draft.examples.putAll(values);
            return this;
        }

        public Builder content(Map<String, MediaType> content) {
            Map<String, MediaType> values = ModelCopies.copyMapEntries(content);
            draft.content.clear();
            draft.content.putAll(values);
            return this;
        }

        public Header build() {
            return draft.copy();
        }
    }
}
