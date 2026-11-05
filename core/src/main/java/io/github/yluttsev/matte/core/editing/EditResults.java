package io.github.yluttsev.matte.core.editing;

import java.util.List;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.ChangeSet;
import io.github.yluttsev.matte.core.model.ApiDocument;

final class EditResults {

    private EditResults() {
    }

    static EditSucceeded succeeded(ApiDocument document, List<Change> changes) {
        return new EditSucceeded(document, new ChangeSet(changes));
    }

    static EditRejected rejected(EditCode code, String message) {
        return new EditRejected(List.of(new EditIssue(code, message)));
    }
}
