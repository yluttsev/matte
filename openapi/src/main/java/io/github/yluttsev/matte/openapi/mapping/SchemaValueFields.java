package io.github.yluttsev.matte.openapi.mapping;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

public record SchemaValueFields(
        boolean hasDefault,
        DocumentValue defaultValue,
        boolean hasExample,
        DocumentValue example,
        List<DocumentValue> examples,
        List<DocumentValue> enumValues,
        boolean hasConst,
        DocumentValue constValue,
        Map<String, BigInteger> integerConstraints,
        List<String> requiredNames
) {

    public SchemaValueFields {
        examples = List.copyOf(examples);
        enumValues = List.copyOf(enumValues);
        integerConstraints = Map.copyOf(integerConstraints);
        requiredNames = List.copyOf(requiredNames);
    }
}
