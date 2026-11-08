package io.github.yluttsev.matte.openapi.testing;

import java.util.function.Supplier;
import io.github.yluttsev.matte.openapi.ImportResult;
import io.github.yluttsev.matte.openapi.OpenApiEditResult;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EditAssertions {

    private EditAssertions() {
    }

    public static OpenApiEditResult.Refused assertRefused(
            ImportResult input,
            Supplier<OpenApiEditResult> operation
    ) {
        byte[] original = input.source().content();
        OpenApiEditResult.Refused refusal = assertInstanceOf(OpenApiEditResult.Refused.class, operation.get());
        assertSame(input, refusal.state());
        assertTrue(refusal.source().isEmpty());
        assertFalse(refusal.message().isBlank());
        assertArrayEquals(original, input.source().content());
        return refusal;
    }
}
