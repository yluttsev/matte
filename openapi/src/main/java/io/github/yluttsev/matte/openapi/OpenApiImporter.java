package io.github.yluttsev.matte.openapi;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import io.github.yluttsev.matte.openapi.parser.JsonSyntaxParser;
import io.github.yluttsev.matte.openapi.parser.SyntaxParser;
import io.github.yluttsev.matte.openapi.parser.YamlSyntaxParser;

public final class OpenApiImporter {

    private static final char UTF8_BOM = 0xFEFF;
    private static final Pattern JSON_SCALAR = Pattern.compile(
            "true|false|null|-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?|\"(?:[^\"\\\\]|\\\\.)*\""
    );
    private static final Map<SourceFormat, SyntaxParser> PARSERS = Map.of(
            SourceFormat.JSON, new JsonSyntaxParser(),
            SourceFormat.YAML, new YamlSyntaxParser()
    );

    public ImportResult importDocument(byte[] content, ImportOptions options) {
        Objects.requireNonNull(options, "options");
        SourceDocument source = new SourceDocument(content);
        if (source.length() == 0) {
            return reject(source, options.formatHint(), ImportDiagnostic.emptySource());
        }
        try {
            return importText(source, options, decode(source));
        } catch (CharacterCodingException exception) {
            return reject(source, options.formatHint(), ImportStatus.INVALID_ENCODING, "Source is not valid UTF-8");
        }
    }

    private static String decode(SourceDocument source) throws CharacterCodingException {
        String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(source.content())).toString();
        if (!text.isEmpty() && text.charAt(0) == UTF8_BOM) {
            return text.substring(1);
        }
        return text;
    }

    private static ImportResult importText(SourceDocument source, ImportOptions options, String text) {
        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            return reject(source, options.formatHint(), ImportDiagnostic.emptySource());
        }
        Optional<SourceFormat> format = options.formatHint().or(() -> detectFormat(trimmed));
        if (format.isEmpty()) {
            return reject(source, format, ImportStatus.AMBIGUOUS_FORMAT,
                    "Specify YAML or JSON for a scalar document");
        }
        return PARSERS.get(format.get()).validate(text)
                .map(diagnostic -> reject(source, format, diagnostic))
                .orElseGet(() -> new ImportResult(source, format, ImportStatus.PARSED, List.of()));
    }

    private static Optional<SourceFormat> detectFormat(String trimmed) {
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return Optional.of(SourceFormat.JSON);
        }
        if (JSON_SCALAR.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        return Optional.of(SourceFormat.YAML);
    }

    private static ImportResult reject(
            SourceDocument source,
            Optional<SourceFormat> format,
            ImportStatus status,
            String message
    ) {
        return reject(source, format, new ImportDiagnostic(status, message));
    }

    private static ImportResult reject(
            SourceDocument source,
            Optional<SourceFormat> format,
            ImportDiagnostic diagnostic
    ) {
        return new ImportResult(source, format, diagnostic.code(), List.of(diagnostic));
    }
}
