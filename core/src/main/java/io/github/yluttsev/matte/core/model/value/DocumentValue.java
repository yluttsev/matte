package io.github.yluttsev.matte.core.model.value;

public sealed interface DocumentValue
        permits NullValue, BooleanValue, NumberValue, StringValue, ArrayValue, ObjectValue {
}
