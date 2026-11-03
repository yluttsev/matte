package io.github.yluttsev.matte.core.editing;

import java.util.Objects;
import io.github.yluttsev.matte.core.editing.change.ChangeSet;
import io.github.yluttsev.matte.core.model.ApiDocument;

public record EditSucceeded(ApiDocument document, ChangeSet changeSet) implements EditResult {

    public EditSucceeded {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(changeSet, "changeSet");
    }
}
