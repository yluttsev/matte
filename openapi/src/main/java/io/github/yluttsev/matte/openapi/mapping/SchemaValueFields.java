package io.github.yluttsev.matte.openapi.mapping;

import java.util.List;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

public record SchemaValueFields(
        boolean hasDefault,
        DocumentValue defaultValue,
        boolean hasExample,
        DocumentValue example,
        List<DocumentValue> examples,
        List<DocumentValue> enumValues,
        boolean hasConst,
        DocumentValue constValue
) {

    public SchemaValueFields {
        examples = List.copyOf(examples);
        enumValues = List.copyOf(enumValues);
    }
}
