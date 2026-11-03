package io.github.yluttsev.matte.openapi;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class SourceDocument {

    private final byte[] content;

    public SourceDocument(byte[] content) {
        this.content = Objects.requireNonNull(content, "content").clone();
    }

    public byte[] content() {
        return content.clone();
    }

    public int length() {
        return content.length;
    }

    public String decodeUtf8() throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content))
                .toString();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SourceDocument document
                && Arrays.equals(content, document.content);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(content);
    }
}
