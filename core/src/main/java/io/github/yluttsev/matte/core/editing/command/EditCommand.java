package io.github.yluttsev.matte.core.editing.command;

public sealed interface EditCommand permits AddSchemaComponent, AddSchemaProperty, RenameSchemaComponent {
}
