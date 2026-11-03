package io.github.yluttsev.matte.openapi;

import java.nio.charset.CharacterCodingException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.openapi.mapping.CoreModelMapper;
import io.github.yluttsev.matte.openapi.parser.JsonSyntaxParser;
import io.github.yluttsev.matte.openapi.parser.SourceStructureReader;
import io.github.yluttsev.matte.openapi.parser.SyntaxParser;
import io.github.yluttsev.matte.openapi.parser.YamlSyntaxParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

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
        String text = source.decodeUtf8();
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
                .orElseGet(() -> importModel(source, format.get(), text));
    }

    private static ImportResult importModel(SourceDocument source, SourceFormat format, String text) {
        Optional<String> version = SourceStructureReader.version(text, format);
        if (version.isEmpty()) {
            return reject(source, Optional.of(format), ImportStatus.INVALID_OPENAPI, "OpenAPI version is missing");
        }
        String versionNumber = version.get();
        Optional<OpenApiVersion> supportedVersion = OpenApiVersion.supported(versionNumber);
        if (supportedVersion.isEmpty()) {
            return reject(source, format, versionNumber, ImportStatus.UNSUPPORTED_VERSION,
                    "Unsupported OpenAPI version: " + versionNumber);
        }
        return interpretModel(source, format, text, versionNumber, supportedVersion.get());
    }

    private static ImportResult interpretModel(
            SourceDocument source,
            SourceFormat format,
            String text,
            String version,
            OpenApiVersion supportedVersion
    ) {
        SwaggerParseResult parsed = parseOpenApi(text);
        List<ImportDiagnostic> diagnostics = parsed.getMessages().stream()
                .map(message -> new ImportDiagnostic(ImportStatus.INVALID_OPENAPI, message))
                .toList();
        if (parsed.getOpenAPI() == null) {
            return invalidParsedModel(source, format, version, diagnostics);
        }
        return verifyParsedModel(parsed.getOpenAPI(), text, format, version)
                .map(diagnostic -> reject(source, format, version, diagnostic))
                .orElseGet(() -> mapModel(source, format, version, supportedVersion, parsed.getOpenAPI(), diagnostics));
    }

    private static ImportResult invalidParsedModel(
            SourceDocument source,
            SourceFormat format,
            String version,
            List<ImportDiagnostic> diagnostics
    ) {
        List<ImportDiagnostic> issues = diagnostics.isEmpty()
                ? List.of(new ImportDiagnostic(ImportStatus.INVALID_OPENAPI,
                        "OpenAPI document could not be interpreted"))
                : diagnostics;
        return new ImportResult(source, Optional.of(format), ImportStatus.INVALID_OPENAPI, issues,
                Optional.of(version), Optional.empty());
    }

    private static ImportResult mapModel(
            SourceDocument source,
            SourceFormat format,
            String version,
            OpenApiVersion supportedVersion,
            OpenAPI openApi,
            List<ImportDiagnostic> diagnostics
    ) {
        try {
            ApiDocument document = new CoreModelMapper().map(openApi, supportedVersion);
            return new ImportResult(source, Optional.of(format), ImportStatus.IMPORTED, diagnostics,
                    Optional.of(version), Optional.of(document));
        } catch (IllegalArgumentException exception) {
            return reject(source, format, version, ImportStatus.MAPPING_ERROR, exception.getMessage());
        }
    }

    private static SwaggerParseResult parseOpenApi(String text) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(false);
        parseOptions.setValidateExternalRefs(false);
        return new OpenAPIV3Parser().readContents(text, null, parseOptions);
    }

    private static Optional<ImportDiagnostic> verifyParsedModel(
            OpenAPI openApi,
            String text,
            SourceFormat format,
            String expectedVersion
    ) {
        if (!expectedVersion.equals(openApi.getOpenapi())) {
            return Optional.of(new ImportDiagnostic(
                    ImportStatus.MAPPING_ERROR,
                    "Parser changed the OpenAPI version"
            ));
        }
        Map<String, ?> parsedSchemas = openApi.getComponents() == null
                ? null
                : openApi.getComponents().getSchemas();
        for (String name : SourceStructureReader.schemaNames(text, format)) {
            if (parsedSchemas == null || !parsedSchemas.containsKey(name)) {
                return Optional.of(new ImportDiagnostic(
                        ImportStatus.MAPPING_ERROR,
                        "Schema component was not interpreted: " + name
                ));
            }
        }
        return Optional.empty();
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

    private static ImportResult reject(
            SourceDocument source,
            SourceFormat format,
            String version,
            ImportStatus status,
            String message
    ) {
        return reject(source, format, version, new ImportDiagnostic(status, message));
    }

    private static ImportResult reject(
            SourceDocument source,
            SourceFormat format,
            String version,
            ImportDiagnostic diagnostic
    ) {
        return new ImportResult(
                source,
                Optional.of(format),
                diagnostic.code(),
                List.of(diagnostic),
                Optional.of(version),
                Optional.empty()
        );
    }
}
