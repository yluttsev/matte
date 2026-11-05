package io.github.yluttsev.matte.core.editing;

import java.util.List;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

final class SchemaCommandHandler {

    private final List<EditCommandHandler<?>> handlers;

    SchemaCommandHandler(List<EditCommandHandler<?>> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    List<EditCommandHandler<?>> handlers() {
        return handlers;
    }

    EditResult apply(
            ApiDocument document,
            DocumentIndex index,
            MutationAccess access,
            EditCommand command
    ) {
        EditCommandHandler<?> selected = null;
        for (EditCommandHandler<?> handler : handlers) {
            if (handler.supports(command)) {
                if (selected != null) {
                    throw new IllegalStateException("Multiple handlers support command: " + command.getClass());
                }
                selected = handler;
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("Unsupported command: " + command.getClass());
        }
        return selected.handleSupported(new EditContext(document, index, access), command);
    }
}
