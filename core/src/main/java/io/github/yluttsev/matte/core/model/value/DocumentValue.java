package io.github.yluttsev.matte.core.model.value;

/** A data value; NullValue represents an explicit null rather than an absent field. */
public sealed interface DocumentValue
        permits NullValue, BooleanValue, NumberValue, StringValue, ArrayValue, ObjectValue {
}
