package io.github.yluttsev.matte.openapi;

public record SourceLocation(int line, int column) {

    public SourceLocation {
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("Line and column must be positive");
        }
    }
}
