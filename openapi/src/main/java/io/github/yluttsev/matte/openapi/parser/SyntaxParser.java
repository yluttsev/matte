package io.github.yluttsev.matte.openapi.parser;

import java.util.Optional;
import io.github.yluttsev.matte.openapi.ImportDiagnostic;

public interface SyntaxParser {

    Optional<ImportDiagnostic> validate(String text);
}
