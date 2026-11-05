package io.github.yluttsev.matte.openapi.patch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceRangeScannerTest {

    @Test
    void testScan_WhenYamlHasNonBmpCharacter_ShouldReturnUtf16Offsets() throws IOException {
        assertFixtureKeyRange("source-ranges-unicode.yaml", new YamlScalarScanner(), "Old:");
    }

    @Test
    void testScan_WhenJsonHasNonBmpCharacter_ShouldReturnUtf16Offsets() throws IOException {
        assertFixtureKeyRange("source-ranges-unicode.json", new JsonScalarScanner(), "\"Old\"");
    }

    @Test
    void testScan_WhenYamlHasBomAndMixedLineEndings_ShouldReturnUtf16Offsets() throws IOException {
        String fixture = new String(TestFixtures.bytes("source-ranges-unicode.yaml"), StandardCharsets.UTF_8);
        String text = Character.toString((char) 0xFEFF) + fixture.replaceFirst("\n", "\r\n");

        assertKeyRange(text, new YamlScalarScanner(), "Old:");
    }

    @Test
    void testScan_WhenYamlHasAlias_ShouldNotReportAnchorRangeTwice() throws IOException {
        String text = new String(TestFixtures.bytes("source-ranges-alias.yaml"), StandardCharsets.UTF_8);

        long aliasedValues = new YamlScalarScanner().scan(text).stream()
                .filter(scalar -> !scalar.key() && scalar.path().startsWith(DocumentPath.schemaComponent("Alias")))
                .count();

        assertEquals(0, aliasedValues);
    }

    @Test
    void testScan_WhenYamlHasDuplicateKeys_ShouldRejectAmbiguousPaths() throws IOException {
        String text = new String(TestFixtures.bytes("duplicate-keys.yaml"), StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> new YamlScalarScanner().scan(text));
    }

    @Test
    void testScan_WhenJsonHasDuplicateKeys_ShouldRejectAmbiguousPaths() throws IOException {
        String text = new String(TestFixtures.bytes("duplicate-keys.json"), StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> new JsonScalarScanner().scan(text));
    }

    private void assertFixtureKeyRange(String fixture, SourceScalarScanner scanner, String token) throws IOException {
        String text = new String(TestFixtures.bytes(fixture), StandardCharsets.UTF_8);
        assertKeyRange(text, scanner, token);
    }

    private void assertKeyRange(String text, SourceScalarScanner scanner, String token) {
        SourceScalar key = scanner.scan(text).stream()
                .filter(scalar -> scalar.key() && scalar.path().equals(DocumentPath.schemaComponent("Old")))
                .findFirst().orElseThrow();

        assertEquals(text.indexOf(token), key.start());
        int expectedEnd = text.indexOf(token) + token.length();
        if (token.endsWith(":")) {
            expectedEnd--;
        }
        assertEquals(expectedEnd, key.end());
    }
}
