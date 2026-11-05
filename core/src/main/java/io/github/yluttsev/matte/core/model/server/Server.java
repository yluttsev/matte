package io.github.yluttsev.matte.core.model.server;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.construction.ModelCopies;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;

public final class Server implements IdentifiedElement, CopyableModel<Server> {

    private final ElementId id;
    private String url;
    private String description;
    private final Map<String, ServerVariable> variables = new LinkedHashMap<>();

    public Server(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private Server(Server source) {
        this(source.id);
        url = source.url;
        description = source.description;
        variables.putAll(ModelCopies.copyModelMap(source.variables));
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public Server copy() {
        return new Server(this);
    }

    public ElementId getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, ServerVariable> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public static final class Builder {

        private final Server draft;

        private Builder(ElementId id) {
            draft = new Server(id);
        }

        public Builder url(String url) {
            draft.url = url;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder variables(Map<String, ServerVariable> variables) {
            Map<String, ServerVariable> values = ModelCopies.copyMapEntries(variables);
            draft.variables.clear();
            draft.variables.putAll(values);
            return this;
        }

        public Server build() {
            return draft.copy();
        }
    }
}
