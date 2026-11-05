package io.github.yluttsev.matte.openapi.patch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;

final class JsonAdditionWriter implements SourceAdditionWriter {

    private static final char UTF8_BOM = 0xFEFF;
    private static final JsonFactory FACTORY = new JsonFactory();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String add(String text, DocumentPath parent, DocumentPath requiredAncestor, String name,
            Object value) {
        Index index = index(text);
        if (!index.objects().containsKey(requiredAncestor)) {
            throw new IllegalArgumentException("Required source mapping was not found: " + requiredAncestor);
        }
        DocumentPath existing = parent;
        while (!index.nodes().contains(existing)) {
            existing = existing.parent().orElseThrow();
        }
        JsonObject object = index.objects().get(existing);
        if (object == null) {
            throw new IllegalArgumentException("Source insertion target is not an object: " + existing);
        }
        if (index.nodes().contains(parent.child(name))) {
            throw new IllegalArgumentException("Source already contains: " + parent.child(name));
        }
        Map<String, Object> content = nested(parent, existing, name, value);
        var entry = content.entrySet().iterator().next();
        return insert(text, object, JSON.writeValueAsString(entry.getKey()),
                JSON.writeValueAsString(entry.getValue()));
    }

    private Map<String, Object> nested(DocumentPath parent, DocumentPath existing, String name,
            Object value) {
        Map<String, Object> current = new LinkedHashMap<>();
        current.put(name, value);
        for (int index = parent.segments().size() - 1; index >= existing.segments().size(); index--) {
            Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put(parent.segments().get(index), current);
            current = wrapped;
        }
        return current;
    }

    private String insert(String text, JsonObject object, String key, String value) {
        boolean multiline = text.substring(object.open(), object.close()).contains("\n");
        int position = object.count() == 0 ? object.open() + 1 : object.lastValueEnd();
        String newline = SourceLineEndings.near(text, position);
        String separator = object.count() == 0 ? "" : ",";
        String member = key + (multiline ? ": " : ":") + value;
        if (multiline) {
            int indent = object.count() == 0
                    ? leadingSpaces(text, object.close()) + 2
                    : leadingSpaces(text, object.lastKeyStart());
            separator += newline + " ".repeat(indent);
        }
        return text.substring(0, position) + separator + member + text.substring(position);
    }

    private int leadingSpaces(String text, int position) {
        int start = text.lastIndexOf('\n', position - 1) + 1;
        int count = 0;
        while (start + count < position && text.charAt(start + count) == ' ') {
            count++;
        }
        return count;
    }

    private Index index(String text) {
        String parseable = !text.isEmpty() && text.charAt(0) == UTF8_BOM ? " " + text.substring(1) : text;
        Map<DocumentPath, JsonObject> objects = new HashMap<>();
        Set<DocumentPath> nodes = new HashSet<>();
        try (JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), parseable)) {
            parser.nextToken();
            visit(parser, DocumentPath.root(), objects, nodes);
        }
        return new Index(objects, nodes);
    }

    private void visit(JsonParser parser, DocumentPath path, Map<DocumentPath, JsonObject> objects,
            Set<DocumentPath> nodes) {
        nodes.add(path);
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            visitObject(parser, path, objects, nodes);
        } else if (parser.currentToken() == JsonToken.START_ARRAY) {
            int index = 0;
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                visit(parser, path.child(Integer.toString(index++)), objects, nodes);
            }
        } else if (parser.currentToken() == JsonToken.VALUE_STRING) {
            parser.getString();
        }
    }

    private void visitObject(JsonParser parser, DocumentPath path, Map<DocumentPath, JsonObject> objects,
            Set<DocumentPath> nodes) {
        int open = Math.toIntExact(parser.currentTokenLocation().getCharOffset());
        int count = 0;
        int lastKeyStart = -1;
        int lastValueEnd = -1;
        while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
            lastKeyStart = Math.toIntExact(parser.currentTokenLocation().getCharOffset());
            String name = parser.currentName();
            if (nodes.contains(path.child(name))) {
                throw new IllegalArgumentException("Duplicate JSON source key: " + name);
            }
            parser.nextToken();
            visit(parser, path.child(name), objects, nodes);
            lastValueEnd = Math.toIntExact(parser.currentLocation().getCharOffset());
            count++;
        }
        int close = Math.toIntExact(parser.currentTokenLocation().getCharOffset());
        objects.put(path, new JsonObject(open, close, count, lastKeyStart, lastValueEnd));
    }

    private record JsonObject(int open, int close, int count, int lastKeyStart, int lastValueEnd) {
    }

    private record Index(Map<DocumentPath, JsonObject> objects, Set<DocumentPath> nodes) {
    }
}
