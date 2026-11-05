package io.github.yluttsev.matte.openapi.patch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

final class JsonScalarScanner implements SourceScalarScanner {

    private static final char UTF8_BOM = 0xFEFF;
    private static final JsonFactory FACTORY = new JsonFactory();

    @Override
    public List<SourceScalar> scan(String text) {
        List<SourceScalar> scalars = new ArrayList<>();
        String parseable = text;
        if (!text.isEmpty() && text.charAt(0) == UTF8_BOM) {
            parseable = " " + text.substring(1);
        }
        try (JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), parseable)) {
            parser.nextToken();
            visit(parser, text, DocumentPath.root(), scalars);
        }
        return scalars;
    }

    private void visit(JsonParser parser, String text, DocumentPath path, List<SourceScalar> scalars) {
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            visitObject(parser, text, path, scalars);
        } else if (parser.currentToken() == JsonToken.START_ARRAY) {
            visitArray(parser, text, path, scalars);
        } else if (parser.currentToken() == JsonToken.VALUE_STRING) {
            scalars.add(scalar(parser, text, path, false, parser.getString()));
        }
    }

    private void visitObject(JsonParser parser, String text, DocumentPath path, List<SourceScalar> scalars) {
        Set<String> names = new HashSet<>();
        while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
            String name = parser.currentName();
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate JSON source key: " + name);
            }
            DocumentPath child = path.child(name);
            scalars.add(scalar(parser, text, child, true, name));
            parser.nextToken();
            visit(parser, text, child, scalars);
        }
    }

    private void visitArray(JsonParser parser, String text, DocumentPath path, List<SourceScalar> scalars) {
        int index = 0;
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            visit(parser, text, path.child(Integer.toString(index++)), scalars);
        }
    }

    private SourceScalar scalar(
            JsonParser parser,
            String text,
            DocumentPath path,
            boolean key,
            String value
    ) {
        int start = Math.toIntExact(parser.currentTokenLocation().getCharOffset());
        int end = quotedEnd(text, start);
        return new SourceScalar(path, key, value, start, end, SourceScalar.QuoteStyle.JSON);
    }

    private int quotedEnd(String text, int start) {
        if (text.charAt(start) != '"') {
            throw new IllegalArgumentException("Expected JSON string token");
        }
        for (int index = start + 1; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '\\') {
                index++;
            } else if (current == '"') {
                return index + 1;
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string token");
    }
}
