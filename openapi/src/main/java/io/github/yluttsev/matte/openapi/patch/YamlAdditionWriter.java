package io.github.yluttsev.matte.openapi.patch;

import java.util.LinkedHashMap;
import java.util.Map;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import tools.jackson.databind.ObjectMapper;

final class YamlAdditionWriter implements SourceAdditionWriter {

    private static final LoadSettings SETTINGS = LoadSettings.builder().build();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String add(String text, DocumentPath parent, DocumentPath requiredAncestor, String name,
            Map<String, Object> schema) {
        Node node = new Compose(SETTINGS).composeString(text).orElseThrow();
        if (!(node instanceof MappingNode)) {
            throw new IllegalArgumentException("OpenAPI source root must be a mapping");
        }
        int existingDepth = 0;
        while (existingDepth < parent.segments().size()) {
            MappingNode mapping = (MappingNode) node;
            Node child = child(mapping, parent.segments().get(existingDepth));
            if (child == null) {
                break;
            }
            if (!(child instanceof MappingNode)) {
                throw new IllegalArgumentException("Source insertion target is not a mapping: " + parent);
            }
            node = child;
            existingDepth++;
        }
        if (existingDepth < requiredAncestor.segments().size()) {
            throw new IllegalArgumentException("Required source mapping was not found: " + requiredAncestor);
        }
        MappingNode mapping = (MappingNode) node;
        if (existingDepth == parent.segments().size() && child(mapping, name) != null) {
            throw new IllegalArgumentException("Source already contains: " + parent.child(name));
        }
        Map<String, Object> content = nested(parent, existingDepth, name, schema);
        if (mapping.getFlowStyle() == FlowStyle.FLOW) {
            return insertFlow(text, mapping, content);
        }
        return insertBlock(text, mapping, content);
    }

    private Node child(MappingNode mapping, String name) {
        for (var entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode key && name.equals(key.getValue())) {
                Node value = entry.getValueNode();
                if (value.getStartMark().orElseThrow().getIndex() < key.getEndMark().orElseThrow().getIndex()) {
                    throw new IllegalArgumentException("Alias-based source mappings cannot be patched");
                }
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> nested(DocumentPath parent, int existingDepth, String name,
            Map<String, Object> schema) {
        Map<String, Object> content = Map.of(name, schema);
        for (int index = parent.segments().size() - 1; index >= existingDepth; index--) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put(parent.segments().get(index), content);
            content = wrapper;
        }
        return content;
    }

    private String insertFlow(String text, MappingNode mapping, Map<String, Object> content) {
        int position = mapping.getEndMark().orElseThrow().getIndex() - 1;
        String serialized = JSON.writeValueAsString(content);
        String member = serialized.substring(1, serialized.length() - 1);
        String separator = mapping.getValue().isEmpty() ? "" : ", ";
        return text.substring(0, position) + separator + member + text.substring(position);
    }

    private String insertBlock(String text, MappingNode mapping, Map<String, Object> content) {
        if (mapping.getValue().isEmpty()) {
            throw new IllegalArgumentException("Empty block mapping cannot be patched");
        }
        var last = mapping.getValue().getLast();
        int lastEnd = last.getValueNode().getEndMark().orElseThrow().getIndex();
        int position = nextLine(text, lastEnd);
        int indentation = indentation(text, last.getKeyNode().getStartMark().orElseThrow().getIndex());
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        String prefix = position == text.length() && !text.endsWith("\n") ? newline : "";
        String addition = render(content, indentation, newline);
        return text.substring(0, position) + prefix + addition + text.substring(position);
    }

    private int nextLine(String text, int end) {
        if (end == text.length() || end > 0 && text.charAt(end - 1) == '\n') {
            return end;
        }
        int lineStart = text.lastIndexOf('\n', end - 1) + 1;
        if (lineStart == end) {
            return end;
        }
        int newline = text.indexOf('\n', end);
        return newline < 0 ? text.length() : newline + 1;
    }

    private int indentation(String text, int position) {
        int lineStart = text.lastIndexOf('\n', position - 1) + 1;
        int count = 0;
        while (lineStart + count < position && text.charAt(lineStart + count) == ' ') {
            count++;
        }
        return count;
    }

    private String render(Map<String, Object> content, int indent, String newline) {
        StringBuilder builder = new StringBuilder();
        for (var entry : content.entrySet()) {
            builder.append(" ".repeat(indent)).append(SourceTextEdits.yamlScalar(entry.getKey())).append(':');
            if (entry.getValue() instanceof Map<?, ?> children && !children.isEmpty()) {
                builder.append(newline);
                builder.append(render(castMap(children), indent + 2, newline));
            } else {
                builder.append(' ').append(scalar(entry.getValue())).append(newline);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> castMap(Map<?, ?> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        input.forEach((key, value) -> result.put((String) key, value));
        return result;
    }

    private String scalar(Object value) {
        if (value instanceof String string) {
            return SourceTextEdits.yamlScalar(string);
        }
        return JSON.writeValueAsString(value);
    }
}
