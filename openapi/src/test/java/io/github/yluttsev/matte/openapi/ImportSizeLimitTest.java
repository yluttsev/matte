package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportSizeLimitTest {

    private static final int PREVIOUS_YAML_CODE_POINT_LIMIT = 3 * 1024 * 1024;
    private static final String PADDING_TOKEN = "__PADDING__";
    private final OpenApiImporter importer = new OpenApiImporter();

    @ParameterizedTest
    @ValueSource(strings = {"minimal.yaml", "minimal.json"})
    void testImportDocument_WhenSizeIsBelowOrAtLimit_ShouldImport(String fixture) throws IOException {
        byte[] source = TestFixtures.bytes(fixture);
        for (int limit : new int[] {source.length, source.length + 1}) {
            ImportResult result = importer.importDocument(source, ImportOptions.autoDetect().withMaxSourceBytes(limit));

            assertEquals(ImportStatus.IMPORTED, result.status(), result.diagnostics().toString());
            assertArrayEquals(source, result.source().content());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"minimal.yaml", "minimal.json"})
    void testImportDocument_WhenSizeExceedsLimitByOne_ShouldRetainBytesAndReportSize(
            String fixture
    ) throws IOException {
        byte[] source = TestFixtures.bytes(fixture);
        int limit = source.length - 1;

        ImportResult result = importer.importDocument(source, ImportOptions.autoDetect().withMaxSourceBytes(limit));

        assertEquals(ImportStatus.SOURCE_TOO_LARGE, result.status());
        assertArrayEquals(source, result.source().content());
        assertTrue(result.document().isEmpty());
        assertTrue(result.version().isEmpty());
        assertTrue(result.format().isEmpty());
        assertEquals(1, result.diagnostics().size());
        ImportDiagnostic diagnostic = result.diagnostics().getFirst();
        assertEquals(ImportStatus.SOURCE_TOO_LARGE, diagnostic.code());
        assertEquals(
                "Source size " + source.length + " bytes exceeds limit " + limit + " bytes",
                diagnostic.message()
        );
        assertTrue(diagnostic.location().isEmpty());
    }

    @Test
    void testImportDocument_WhenOversizedSourceIsInvalidUtf8_ShouldRejectBeforeDecoding() {
        byte[] source = {(byte) 0xFF, (byte) 0xFE};

        ImportResult result = importer.importDocument(source, ImportOptions.withFormat(SourceFormat.YAML)
                .withMaxSourceBytes(1));

        assertEquals(ImportStatus.SOURCE_TOO_LARGE, result.status());
        assertEquals(Optional.of(SourceFormat.YAML), result.format());
        assertArrayEquals(source, result.source().content());
    }

    @ParameterizedTest
    @ValueSource(strings = {"size-unicode.yaml", "size-unicode.json"})
    void testImportDocument_WhenUtf8UsesMultipleBytes_ShouldCountBytes(String fixture) throws IOException {
        byte[] source = TestFixtures.bytes(fixture);
        String text = new String(source, StandardCharsets.UTF_8);

        ImportResult result = importer.importDocument(source,
                ImportOptions.autoDetect().withMaxSourceBytes(text.codePointCount(0, text.length())));

        assertEquals(ImportStatus.SOURCE_TOO_LARGE, result.status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"minimal.yaml", "minimal.json"})
    void testImportDocument_WhenDefaultLimitExceeded_ShouldReportSizeBeforeSyntax(String fixture) throws IOException {
        byte[] source = Arrays.copyOf(TestFixtures.bytes(fixture), ImportOptions.DEFAULT_MAX_SOURCE_BYTES + 1);

        ImportResult result = importer.importDocument(source, ImportOptions.autoDetect());

        assertEquals(ImportStatus.SOURCE_TOO_LARGE, result.status());
        assertArrayEquals(source, result.source().content());
    }

    @Test
    void testImportDocument_WhenYamlExceedsPreviousParserLimit_ShouldReportSize() throws IOException {
        byte[] source = Arrays.copyOf(TestFixtures.bytes("minimal.yaml"), PREVIOUS_YAML_CODE_POINT_LIMIT + 1);
        Arrays.fill(source, TestFixtures.bytes("minimal.yaml").length, source.length, (byte) ' ');

        ImportResult result = importer.importDocument(source, ImportOptions.autoDetect());

        assertEquals(ImportStatus.SOURCE_TOO_LARGE, result.status());
        assertEquals(ImportStatus.SOURCE_TOO_LARGE, result.diagnostics().getFirst().code());
    }

    @Test
    void testImportOptions_WhenLimitIsOutsideSupportedRange_ShouldRejectCallerError() {
        assertThrows(IllegalArgumentException.class, () -> ImportOptions.autoDetect().withMaxSourceBytes(0));
        assertThrows(IllegalArgumentException.class, () -> ImportOptions.autoDetect().withMaxSourceBytes(-1));
        assertThrows(IllegalArgumentException.class, () -> ImportOptions.autoDetect()
                .withMaxSourceBytes(ImportOptions.MAX_SUPPORTED_SOURCE_BYTES + 1));
        assertEquals(ImportOptions.DEFAULT_MAX_SOURCE_BYTES, ImportOptions.autoDetect().maxSourceBytes());
        assertEquals(ImportOptions.DEFAULT_MAX_SOURCE_BYTES, new ImportOptions(Optional.empty()).maxSourceBytes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"large-source.yaml", "large-source.json"})
    void testImportDocument_WhenSizeIsOneByteBelowDefaultLimit_ShouldImport(String fixture) throws IOException {
        String template = new String(TestFixtures.bytes(fixture), StandardCharsets.UTF_8);
        int paddingLength = ImportOptions.DEFAULT_MAX_SOURCE_BYTES - 1
                - template.getBytes(StandardCharsets.UTF_8).length + PADDING_TOKEN.length();
        byte[] source = template.replace(PADDING_TOKEN, "a".repeat(paddingLength)).getBytes(StandardCharsets.UTF_8);

        ImportResult result = importer.importDocument(source, ImportOptions.autoDetect());

        assertEquals(ImportOptions.DEFAULT_MAX_SOURCE_BYTES - 1, source.length);
        assertEquals(ImportStatus.IMPORTED, result.status(), result.diagnostics().toString());
        assertArrayEquals(source, result.source().content());
    }
}
