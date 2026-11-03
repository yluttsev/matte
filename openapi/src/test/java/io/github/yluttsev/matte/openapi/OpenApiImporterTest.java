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
        assertEquals(ImportStatus.IMPORTED, result.status());
        assertTrue(result.document().isPresent());
    }

    @Test
    void testImportDocument_WhenFormatNotSpecified_ShouldDetectJson() throws IOException {
        byte[] content = TestFixtures.bytes("minimal.json");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertArrayEquals(content, result.source().content());
        assertEquals(Optional.of(SourceFormat.JSON), result.format());
        assertEquals(ImportStatus.IMPORTED, result.status());
    }

    @Test
    void testImportDocument_WhenInputIsInvalidSyntax_ShouldRetainItAndReportLocation()
            throws IOException {
        byte[] content = TestFixtures.bytes("invalid.json");

        ImportResult result = importer.importDocument(content, ImportOptions.withFormat(SourceFormat.JSON));

        assertArrayEquals(content, result.source().content());
        assertEquals(ImportStatus.SYNTAX_ERROR, result.status());
        assertTrue(result.diagnostics().getFirst().location().isPresent());
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
        assertEquals(ImportStatus.EMPTY_SOURCE, result.status());
    }

    @Test
    void testImportDocument_WhenInputAndOutputArraysChange_ShouldKeepOriginalBytes() throws IOException {
        byte[] content = TestFixtures.bytes("minimal.json");
        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());
        content[0] = 42;
        byte[] returned = result.source().content();
        returned[1] = 42;

        assertArrayEquals(TestFixtures.bytes("minimal.json"), result.source().content());
    }

    @Test
    void testImportDocument_WhenUtf8IsInvalid_ShouldKeepBytesAndReportEncoding() throws IOException {
        byte[] content = TestFixtures.bytes("arbitrary-bytes.bin");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertArrayEquals(content, result.source().content());
        assertEquals(ImportStatus.INVALID_ENCODING, result.status());
        assertEquals(Optional.empty(), result.format());
    }

    @Test
    void testImportDocument_WhenYamlHasDuplicateKeys_ShouldReportTheirLocation() throws IOException {
        byte[] content = TestFixtures.bytes("duplicate-keys.yaml");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(Optional.of(SourceFormat.YAML), result.format());
        assertEquals(ImportStatus.DUPLICATE_KEY, result.status());
        assertEquals(Optional.of(new SourceLocation(4, 3)), result.diagnostics().getFirst().location());
    }

    @Test
    void testImportDocument_WhenJsonHasDuplicateKeys_ShouldReportError() throws IOException {
        byte[] content = TestFixtures.bytes("duplicate-keys.json");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(ImportStatus.DUPLICATE_KEY, result.status());
        assertTrue(result.diagnostics().getFirst().location().isPresent());
    }

    @Test
    void testImportDocument_WhenJsonHasComments_ShouldNotParseAsYaml() throws IOException {
        byte[] content = TestFixtures.bytes("json-with-comments.json");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(Optional.of(SourceFormat.JSON), result.format());
        assertEquals(ImportStatus.SYNTAX_ERROR, result.status());
        assertArrayEquals(content, result.source().content());
    }

    @Test
    void testImportDocument_WhenJsonHasTrailingComma_ShouldRejectIt() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("trailing-comma.json"),
                ImportOptions.autoDetect());

        assertEquals(ImportStatus.SYNTAX_ERROR, result.status());
    }

    @Test
    void testImportDocument_WhenYamlIsMalformed_ShouldReportSyntaxError() throws IOException {
        byte[] content = TestFixtures.bytes("invalid.yaml");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(Optional.of(SourceFormat.YAML), result.format());
        assertEquals(ImportStatus.SYNTAX_ERROR, result.status());
        assertTrue(result.diagnostics().getFirst().location().isPresent());
        assertArrayEquals(content, result.source().content());
    }

    @Test
    void testImportDocument_WhenYamlStartsWithBrace_ShouldRespectExplicitHint() throws IOException {
        byte[] content = TestFixtures.bytes("flow-mapping.yaml");

        ImportResult hinted = importer.importDocument(content, ImportOptions.withFormat(SourceFormat.YAML));
        ImportResult detected = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(ImportStatus.INVALID_OPENAPI, hinted.status());
        assertEquals(ImportStatus.SYNTAX_ERROR, detected.status());
        assertEquals(Optional.of(SourceFormat.JSON), detected.format());
    }

    @Test
    void testImportDocument_WhenYamlHasCyclicAlias_ShouldNotLoop() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("cyclic-alias.yaml"),
                ImportOptions.withFormat(SourceFormat.YAML));

        assertEquals(ImportStatus.IMPORTED, result.status());
    }

    @Test
    void testImportDocument_WhenYamlHasMultipleDocuments_ShouldRejectThem() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("multiple-documents.yaml"),
                ImportOptions.withFormat(SourceFormat.YAML));

        assertEquals(ImportStatus.MULTIPLE_DOCUMENTS, result.status());
    }

    @Test
    void testImportDocument_WhenJsonHasMultipleValues_ShouldRejectThem() throws IOException {
        ImportResult result = importer.importDocument(TestFixtures.bytes("multiple-values.json"),
                ImportOptions.withFormat(SourceFormat.JSON));

        assertEquals(ImportStatus.MULTIPLE_DOCUMENTS, result.status());
    }

    @Test
    void testImportDocument_WhenScalarFormatIsAmbiguous_ShouldRequireHint() throws IOException {
        byte[] content = TestFixtures.bytes("scalar.json");

        ImportResult ambiguous = importer.importDocument(content, ImportOptions.autoDetect());
        ImportResult explicit = importer.importDocument(content, ImportOptions.withFormat(SourceFormat.JSON));

        assertEquals(ImportStatus.AMBIGUOUS_FORMAT, ambiguous.status());
        assertEquals(ImportStatus.INVALID_OPENAPI, explicit.status());
    }

    @Test
    void testImportDocument_WhenYamlStartsWithQuotedKey_ShouldDetectYaml() throws IOException {
        byte[] content = TestFixtures.bytes("quoted-key.yaml");

        ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

        assertEquals(Optional.of(SourceFormat.YAML), result.format());
        assertEquals(ImportStatus.IMPORTED, result.status());
    }

    @Test
    void testImportDocument_WhenOptionsOrSourceAreNull_ShouldRejectThem() {
        assertThrows(NullPointerException.class, () -> importer.importDocument(null, ImportOptions.autoDetect()));
        assertThrows(NullPointerException.class, () -> importer.importDocument(new byte[0], null));
        assertThrows(NullPointerException.class, () -> ImportOptions.withFormat(null));
    }
}
