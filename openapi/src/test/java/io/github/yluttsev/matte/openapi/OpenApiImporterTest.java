package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.util.Optional;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiImporterTest {

    private final OpenApiImporter importer = new OpenApiImporter();

    @Test
    void testImportDocument_WhenSourceContainsBomAndCrlf_ShouldPreserveExactBytes() throws IOException {
        byte[] content = TestFixtures.bytes("bom-crlf.yaml");

        ImportResult result = importer.importDocument(content, ImportOptions.withFormat(SourceFormat.YAML));

        assertArrayEquals(content, result.source().content());
        assertEquals(Optional.of(SourceFormat.YAML), result.format());
        assertEquals(ImportStatus.NOT_PARSED, result.status());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void testImportDocument_WhenFormatNotSpecified_ShouldNotGuessUntilParsingIsImplemented() throws IOException {
        byte[] content = TestFixtures.bytes("minimal.json");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertArrayEquals(content, result.source().content());
        assertEquals(Optional.empty(), result.format());
        assertEquals(ImportStatus.NOT_PARSED, result.status());
    }

    @Test
    void testImportDocument_WhenInputIsInvalidSyntax_ShouldRetainItWithoutClaimingToHaveParsedIt()
            throws IOException {
        byte[] content = TestFixtures.bytes("invalid.json");

        ImportResult result = importer.importDocument(content, ImportOptions.withFormat(SourceFormat.JSON));

        assertArrayEquals(content, result.source().content());
        assertEquals(ImportStatus.NOT_PARSED, result.status());
    }

    @Test
    void testImportDocument_WhenSourceIsEmpty_ShouldReturnDiagnosticAndKeepBytes() throws IOException {
        byte[] content = TestFixtures.bytes("empty.yaml");
        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertArrayEquals(content, result.source().content());
        assertEquals(ImportStatus.EMPTY_SOURCE, result.status());
        assertEquals(ImportStatus.EMPTY_SOURCE, result.diagnostics().getFirst().code());
    }

    @Test
    void testImportDocument_WhenSourceIsWhitespaceOnly_ShouldNotTreatItAsEmptyBytes() throws IOException {
        byte[] content = TestFixtures.bytes("whitespace.yaml");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertArrayEquals(content, result.source().content());
        assertEquals(ImportStatus.NOT_PARSED, result.status());
    }

    @Test
    void testImportDocument_WhenInputAndOutputArraysChange_ShouldKeepOriginalBytes() throws IOException {
        byte[] content = TestFixtures.bytes("arbitrary-bytes.bin");
        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());
        content[0] = 42;
        byte[] returned = result.source().content();
        returned[1] = 42;

        assertArrayEquals(TestFixtures.bytes("arbitrary-bytes.bin"), result.source().content());
    }

    @Test
    void testImportDocument_WhenOptionsOrSourceAreNull_ShouldRejectThem() {
        assertThrows(NullPointerException.class, () -> importer.importDocument(null, ImportOptions.autoDetect()));
        assertThrows(NullPointerException.class, () -> importer.importDocument(new byte[0], null));
        assertThrows(NullPointerException.class, () -> ImportOptions.withFormat(null));
    }
}
