package io.github.yluttsev.matte.core.editing;

import io.github.yluttsev.matte.core.editing.command.EditCommand;

public interface EditCommandHandler<C extends EditCommand> {

    Class<C> commandType();

    default boolean supports(EditCommand command) {
        return command.getClass() == commandType();
    }

    EditResult handle(EditContext context, C command);

    default EditResult handleSupported(EditContext context, EditCommand command) {
        return handle(context, commandType().cast(command));
    }
}
