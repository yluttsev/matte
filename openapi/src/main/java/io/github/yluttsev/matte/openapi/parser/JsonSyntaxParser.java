package io.github.yluttsev.matte.openapi.parser;

import java.util.Objects;
import java.util.Optional;
import io.github.yluttsev.matte.openapi.ImportDiagnostic;
import io.github.yluttsev.matte.openapi.ImportStatus;
import io.github.yluttsev.matte.openapi.SourceLocation;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.json.JsonFactory;

public final class JsonSyntaxParser implements SyntaxParser {

    private static final String DUPLICATE_FIELD_PREFIX = "Duplicate Object property";
    private static final String MULTIPLE_VALUES_MESSAGE = "JSON must contain exactly one value";
    private static final JsonFactory FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    @Override
    public Optional<ImportDiagnostic> validate(String text) {
        try (JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), text)) {
            JsonToken first = parser.nextToken();
            if (first == null) {
                return Optional.of(ImportDiagnostic.emptySource());
            }
            parser.skipChildren();
            if (parser.nextToken() != null) {
                return Optional.of(new ImportDiagnostic(ImportStatus.MULTIPLE_DOCUMENTS, MULTIPLE_VALUES_MESSAGE));
            }
            return Optional.empty();
        } catch (JacksonException exception) {
            return Optional.of(toDiagnostic(exception));
        }
    }

    private static ImportDiagnostic toDiagnostic(JacksonException exception) {
        TokenStreamLocation location = exception.getLocation();
        SourceLocation position = new SourceLocation(
                Math.max(1, location.getLineNr()),
                Math.max(1, location.getColumnNr())
        );
        String message = Objects.requireNonNullElse(exception.getOriginalMessage(), "Invalid JSON syntax");
        ImportStatus status = message.startsWith(DUPLICATE_FIELD_PREFIX)
                ? ImportStatus.DUPLICATE_KEY
                : ImportStatus.SYNTAX_ERROR;
        return new ImportDiagnostic(status, message, Optional.of(position));
    }
}
