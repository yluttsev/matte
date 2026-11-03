package io.github.yluttsev.matte.core.model.media;

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
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

public final class MediaType implements IdentifiedElement, CopyableModel<MediaType>, ReferenceOwner {

    private final ElementId id;
    private Schema schema;
    private DocumentValue example;
    private final Map<String, ReferenceOr<Example>> examples = new LinkedHashMap<>();
    private final Map<String, Encoding> encoding = new LinkedHashMap<>();

    public MediaType(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private MediaType(MediaType source) {
        this(source.id);
        schema = ModelCopies.copy(source.schema);
        example = source.example;
        examples.putAll(ModelCopies.map(source.examples));
        encoding.putAll(ModelCopies.map(source.encoding));
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public MediaType copy() {
        return new MediaType(this);
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

    @Override
    public void rewriteReferences(MutationAccess access, UnaryOperator<String> transform) {
        access.require(this);
        ReferenceUpdates.map(examples, transform);
    }

    public static final class Builder {

        private final MediaType draft;

        private Builder(ElementId id) {
            draft = new MediaType(id);
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

        public Builder encoding(Map<String, Encoding> encoding) {
            Map<String, Encoding> values = ModelCopies.valueMap(encoding);
            draft.encoding.clear();
            draft.encoding.putAll(values);
            return this;
        }

        public MediaType build() {
            return draft.copy();
        }
    }
}
