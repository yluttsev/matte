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
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.ParameterStyle;
import io.github.yluttsev.matte.core.model.reference.ReferenceOr;

public final class Encoding implements IdentifiedElement, CopyableModel<Encoding>, ReferenceOwner {

    private final ElementId id;
    private String contentType;
    private final Map<String, ReferenceOr<Header>> headers = new LinkedHashMap<>();
    private ParameterStyle style;
    private Boolean explode;
    private Boolean allowReserved;

    public Encoding(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private Encoding(Encoding source) {
        this(source.id);
        contentType = source.contentType;
        headers.putAll(ModelCopies.map(source.headers));
        style = source.style;
        explode = source.explode;
        allowReserved = source.allowReserved;
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public Encoding copy() {
        return new Encoding(this);
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

    @Override
    public void rewriteReferences(MutationAccess access, UnaryOperator<String> transform) {
        access.require(this);
        ReferenceUpdates.map(headers, transform);
    }

    public static final class Builder {

        private final Encoding draft;

        private Builder(ElementId id) {
            draft = new Encoding(id);
        }

        public Builder contentType(String contentType) {
            draft.contentType = contentType;
            return this;
        }

        public Builder headers(Map<String, ReferenceOr<Header>> headers) {
            Map<String, ReferenceOr<Header>> values = ModelCopies.valueMap(headers);
            draft.headers.clear();
            draft.headers.putAll(values);
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

        public Encoding build() {
            return draft.copy();
        }
    }
}
