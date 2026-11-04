package io.github.yluttsev.matte.openapi.patch;

final class YamlSourceOffsets {

    private final int[] charOffsets;

    YamlSourceOffsets(String text) {
        int codePoints = text.codePointCount(0, text.length());
        charOffsets = new int[codePoints + 1];
        int position = 0;
        for (int index = 0; index < codePoints; index++) {
            charOffsets[index] = position;
            position += Character.charCount(text.codePointAt(position));
        }
        charOffsets[codePoints] = position;
    }

    int charOffset(int parserIndex) {
        if (parserIndex < 0 || parserIndex >= charOffsets.length) {
            throw new IllegalArgumentException("YAML source offset is outside the document: " + parserIndex);
        }
        return charOffsets[parserIndex];
    }
}
