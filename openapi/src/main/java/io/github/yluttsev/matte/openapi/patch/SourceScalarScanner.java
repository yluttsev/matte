package io.github.yluttsev.matte.openapi.patch;

import java.util.List;

interface SourceScalarScanner {

    List<SourceScalar> scan(String text);
}
