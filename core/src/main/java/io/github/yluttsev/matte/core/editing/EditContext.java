package io.github.yluttsev.matte.core.editing;

import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;

public record EditContext(ApiDocument document, DocumentIndex index, MutationAccess access) {
}
