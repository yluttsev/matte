package io.github.yluttsev.matte.openapi.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static byte[] bytes(String name) throws IOException {
        try (InputStream stream = TestFixtures.class.getResourceAsStream("/fixtures/" + name)) {
            return Objects.requireNonNull(stream, name).readAllBytes();
        }
    }
}
