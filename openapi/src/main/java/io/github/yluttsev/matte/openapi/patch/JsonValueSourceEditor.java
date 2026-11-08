package io.github.yluttsev.matte.openapi.patch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.parser.ParserSettings;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;

final class JsonValueSourceEditor implements SchemaValueSourceEditor {

    private static final char UTF8_BOM = 0xFEFF;
    private static final JsonFactory FACTORY = new JsonFactory();
    private static final ObjectMapper JSON = ParserSettings.json();

    @Override
    public String replace(String text, DocumentPath path, Object value) {
        Range range = index(text).values().get(path);
        if (range == null) {
            throw new IllegalArgumentException("Source value was not found: " + path);
        }
        return text.substring(0, range.start()) + JSON.writeValueAsString(value) + text.substring(range.end());
    }

    @Override
    public String remove(String text, DocumentPath path) {
        Index index = index(text);
        DocumentPath parentPath = path.parent().orElseThrow();
        ObjectRange parent = index.objects().get(parentPath);
        if (parent == null) {
            throw new IllegalArgumentException("Source parent is not an object: " + parentPath);
        }
        MemberRange member = member(parent, path.segments().getLast());
        int memberIndex = parent.members().indexOf(member);
        if (parent.members().size() == 1) {
            return text.substring(0, member.keyStart()) + text.substring(member.valueEnd());
        }
        if (memberIndex < parent.members().size() - 1) {
            int comma = commaBetween(text, member.valueEnd(), parent.members().get(memberIndex + 1).keyStart());
            return text.substring(0, member.keyStart()) + text.substring(comma + 1);
        }
        int comma = commaBetween(text, parent.members().get(memberIndex - 1).valueEnd(), member.keyStart());
        return text.substring(0, comma) + text.substring(member.valueEnd());
    }

    private Index index(String text) {
        String parseable = text;
        if (!text.isEmpty() && text.charAt(0) == UTF8_BOM) {
            parseable = " " + text.substring(1);
        }
        Map<DocumentPath, Range> values = new HashMap<>();
        Map<DocumentPath, ObjectRange> objects = new HashMap<>();
        try (JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), parseable)) {
            parser.nextToken();
            visit(parser, DocumentPath.root(), values, objects);
        }
        return new Index(values, objects);
    }

    private Range visit(
            JsonParser parser,
            DocumentPath path,
            Map<DocumentPath, Range> values,
            Map<DocumentPath, ObjectRange> objects
    ) {
        int start = Math.toIntExact(parser.currentTokenLocation().getCharOffset());
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            return visitObject(parser, path, start, values, objects);
        }
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            return visitArray(parser, path, start, values, objects);
        }
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            parser.getString();
        }
        Range range = new Range(start, Math.toIntExact(parser.currentLocation().getCharOffset()));
        values.put(path, range);
        return range;
    }

    private Range visitObject(
            JsonParser parser,
            DocumentPath path,
            int start,
            Map<DocumentPath, Range> values,
            Map<DocumentPath, ObjectRange> objects
    ) {
        List<MemberRange> members = new ArrayList<>();
        Set<String> names = new HashSet<>();
        while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
            int keyStart = Math.toIntExact(parser.currentTokenLocation().getCharOffset());
            String name = parser.currentName();
            if (!names.add(name)) {
                throw new IllegalArgumentException("Duplicate JSON source key: " + name);
            }
            parser.nextToken();
            Range value = visit(parser, path.child(name), values, objects);
            members.add(new MemberRange(name, keyStart, value.end()));
        }
        Range range = new Range(start, Math.toIntExact(parser.currentLocation().getCharOffset()));
        values.put(path, range);
        objects.put(path, new ObjectRange(List.copyOf(members)));
        return range;
    }

    private Range visitArray(
            JsonParser parser,
            DocumentPath path,
            int start,
            Map<DocumentPath, Range> values,
            Map<DocumentPath, ObjectRange> objects
    ) {
        int itemIndex = 0;
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            visit(parser, path.child(Integer.toString(itemIndex++)), values, objects);
        }
        Range range = new Range(start, Math.toIntExact(parser.currentLocation().getCharOffset()));
        values.put(path, range);
        return range;
    }

    private MemberRange member(ObjectRange parent, String name) {
        return parent.members().stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Source field was not found: " + name));
    }

    private int commaBetween(String text, int start, int end) {
        int comma = text.indexOf(',', start);
        if (comma < 0 || comma >= end) {
            throw new IllegalArgumentException("JSON member separator was not found");
        }
        return comma;
    }

    private record Range(int start, int end) {
    }

    private record MemberRange(String name, int keyStart, int valueEnd) {
    }

    private record ObjectRange(List<MemberRange> members) {
    }

    private record Index(Map<DocumentPath, Range> values, Map<DocumentPath, ObjectRange> objects) {
    }
}
