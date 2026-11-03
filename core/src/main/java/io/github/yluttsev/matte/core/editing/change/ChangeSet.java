package io.github.yluttsev.matte.core.editing.change;

import java.util.List;

public record ChangeSet(List<Change> changes) {

    public ChangeSet {
        changes = List.copyOf(changes);
    }
}
