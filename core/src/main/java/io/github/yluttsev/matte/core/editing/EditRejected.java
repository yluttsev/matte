package io.github.yluttsev.matte.core.editing;

import java.util.List;

public record EditRejected(List<EditIssue> issues) implements EditResult {

    public EditRejected {
        issues = List.copyOf(issues);
        if (issues.isEmpty()) {
            throw new IllegalArgumentException("Rejected edit must contain at least one issue");
        }
    }
}
