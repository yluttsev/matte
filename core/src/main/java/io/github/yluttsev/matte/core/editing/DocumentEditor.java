package io.github.yluttsev.matte.core.editing;

import java.util.List;
import java.util.Objects;
import io.github.yluttsev.matte.core.construction.DocumentStructureValidator;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

public final class DocumentEditor {

    public EditResult apply(ApiDocument document, EditCommand command) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(command, "command");
        ApiDocument working = document.copy();
        DocumentIndex index = DocumentIndex.build(working);
        MutationAccess access = new MutationAccess(working);
        try {
            EditResult result = new SchemaCommandHandler().apply(working, index, access, command);
            if (result instanceof EditRejected) {
                return result;
            }
            try {
                DocumentStructureValidator.validate(working);
            } catch (IllegalArgumentException exception) {
                return new EditRejected(List.of(new EditIssue(EditCode.INVALID_STRUCTURE, exception.getMessage())));
            }
            return result;
        } finally {
            access.close();
        }
    }
}
