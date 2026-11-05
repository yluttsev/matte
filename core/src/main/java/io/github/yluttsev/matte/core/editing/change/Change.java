package io.github.yluttsev.matte.core.editing.change;

public sealed interface Change
        permits SchemaComponentAdded,
        SchemaPropertyAdded,
        SchemaComponentRenamed,
        ReferenceChanged,
        SchemaObjectUpdated,
        SchemaPropertyRenamed,
        SchemaPropertyDeleted,
        SchemaComponentDeleted {
}
