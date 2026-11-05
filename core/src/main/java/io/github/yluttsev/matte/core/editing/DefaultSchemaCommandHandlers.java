package io.github.yluttsev.matte.core.editing;

import java.util.List;

final class DefaultSchemaCommandHandlers {

    private DefaultSchemaCommandHandlers() {
    }

    static List<EditCommandHandler<?>> create() {
        return List.of(
                new AddSchemaComponentHandler(),
                new AddSchemaPropertyHandler(),
                new RenameSchemaComponentHandler(),
                new RenameSchemaPropertyHandler(),
                new DeleteSchemaPropertyHandler(),
                new DeleteSchemaComponentHandler(),
                new UpdateSchemaObjectHandler()
        );
    }
}
