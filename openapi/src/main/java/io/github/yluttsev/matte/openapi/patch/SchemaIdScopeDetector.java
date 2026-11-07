package io.github.yluttsev.matte.openapi.patch;

import java.nio.charset.CharacterCodingException;
import java.util.Map;
import java.util.Objects;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.github.yluttsev.matte.openapi.SourceDocument;
import io.github.yluttsev.matte.openapi.SourceFormat;
import io.github.yluttsev.matte.openapi.parser.SourceStructureReader;

public final class SchemaIdScopeDetector {

    private static final char UTF8_BOM = 0xFEFF;
    public static final String UNSUPPORTED_EDIT_MESSAGE
            = "Schema rename and deletion are not supported while OpenAPI 3.1 schema $id scopes are present";
    private static final Map<SourceFormat, SourceScalarScanner> SCANNERS = Map.of(
            SourceFormat.JSON,
            new JsonScalarScanner(),
            SourceFormat.YAML,
            new YamlScalarScanner()
    );

    private SchemaIdScopeDetector() {
    }

    public static boolean containsSchemaId(SourceDocument source, SourceFormat format) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        String text = withoutBom(decode(source));
        if (!isOpenApi31(text, format)) {
            return false;
        }
        return SCANNERS.get(format).scan(text).stream().anyMatch(SchemaReferenceRelocator::isPotentialSchemaId);
    }

    public static void rejectIfPresent(SourceDocument source, SourceFormat format) {
        if (containsSchemaId(source, format)) {
            throw new IllegalArgumentException(UNSUPPORTED_EDIT_MESSAGE);
        }
    }

    private static boolean isOpenApi31(String text, SourceFormat format) {
        return SourceStructureReader.version(text, format)
                .flatMap(OpenApiVersion::supported)
                .filter(version -> version == OpenApiVersion.V3_1)
                .isPresent();
    }

    private static String withoutBom(String text) {
        if (!text.isEmpty() && text.charAt(0) == UTF8_BOM) {
            return text.substring(1);
        }
        return text;
    }

    private static String decode(SourceDocument source) {
        try {
            return source.decodeUtf8();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Source is not valid UTF-8", exception);
        }
    }
}
