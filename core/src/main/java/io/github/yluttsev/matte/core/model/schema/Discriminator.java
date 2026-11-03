package io.github.yluttsev.matte.core.model.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;
import io.github.yluttsev.matte.core.editing.MutationAccess;
import io.github.yluttsev.matte.core.editing.ReferenceOwner;

public final class Discriminator implements CopyableModel<Discriminator>, ReferenceOwner {

    private String propertyName;
    private final Map<String, SchemaReference> mapping = new LinkedHashMap<>();

    public Discriminator() {
    }

    private Discriminator(Discriminator source) {
        propertyName = source.propertyName;
        mapping.putAll(ModelCopies.valueMap(source.mapping));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Discriminator copy() {
        return new Discriminator(this);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Map<String, SchemaReference> getMapping() {
        return Collections.unmodifiableMap(mapping);
    }

    @Override
    public void rewriteReferences(MutationAccess access, UnaryOperator<String> transform) {
        access.require(this);
        mapping.replaceAll((name, reference) -> new SchemaReference(transform.apply(reference.value())));
    }

    public static final class Builder {

        private final Discriminator draft = new Discriminator();

        private Builder() {
        }

        public Builder propertyName(String propertyName) {
            draft.propertyName = propertyName;
            return this;
        }

        public Builder mapping(Map<String, SchemaReference> mapping) {
            Map<String, SchemaReference> values = ModelCopies.valueMap(mapping);
            draft.mapping.clear();
            draft.mapping.putAll(values);
            return this;
        }

        public Discriminator build() {
            return draft.copy();
        }
    }
}
