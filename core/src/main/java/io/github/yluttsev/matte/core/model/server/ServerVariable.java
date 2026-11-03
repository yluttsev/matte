package io.github.yluttsev.matte.core.model.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.github.yluttsev.matte.core.construction.CopyableModel;

public final class ServerVariable implements CopyableModel<ServerVariable> {

    private String defaultValue;
    private String description;
    private final List<String> enumValues = new ArrayList<>();

    public ServerVariable() {
    }

    private ServerVariable(ServerVariable source) {
        defaultValue = source.defaultValue;
        description = source.description;
        enumValues.addAll(List.copyOf(source.enumValues));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ServerVariable copy() {
        return new ServerVariable(this);
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEnumValues() {
        return Collections.unmodifiableList(enumValues);
    }

    public static final class Builder {

        private final ServerVariable draft = new ServerVariable();

        private Builder() {
        }

        public Builder defaultValue(String defaultValue) {
            draft.defaultValue = defaultValue;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder enumValues(List<String> enumValues) {
            List<String> values = List.copyOf(enumValues);
            draft.enumValues.clear();
            draft.enumValues.addAll(values);
            return this;
        }

        public ServerVariable build() {
            return draft.copy();
        }
    }
}
