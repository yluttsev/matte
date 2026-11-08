package io.github.yluttsev.matte.openapi.patch;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceEditCode;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import tools.jackson.databind.ObjectMapper;

final class YamlAdditionWriter implements SourceAdditionWriter {

    private static final LoadSettings SETTINGS = LoadSettings.builder().build();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String add(
            String text,
            DocumentPath parent,
            DocumentPath requiredAncestor,
            String name,
            Object value
    ) {
        Node node = new Compose(SETTINGS).composeString(text).orElseThrow();
        YamlSourceOffsets offsets = new YamlSourceOffsets(text);
        if (!(node instanceof MappingNode)) {
            throw new SourceEditRefusal(SourceEditCode.AMBIGUOUS_LAYOUT, "OpenAPI source root must be a mapping");
        }
        YamlAliasSafety.requireUniqueInsertionPath(node, parent);
        int existingDepth = 0;
        while (existingDepth < parent.segments().size()) {
            MappingNode mapping = (MappingNode) node;
            Node child = child(mapping, parent.segments().get(existingDepth));
            if (child == null) {
                break;
            }
            if (!(child instanceof MappingNode)) {
                throw new SourceEditRefusal(
                        SourceEditCode.AMBIGUOUS_LAYOUT,
                        "Source insertion target is not a mapping: " + parent
                );
            }
            node = child;
            existingDepth++;
        }
        if (existingDepth < requiredAncestor.segments().size()) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required source mapping was not found: " + requiredAncestor
            );
        }
        MappingNode mapping = (MappingNode) node;
        if (existingDepth == parent.segments().size() && child(mapping, name) != null) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Source already contains: " + parent.child(name)
            );
        }
        Map<String, Object> content = nested(parent, existingDepth, name, value);
        if (mapping.getFlowStyle() == FlowStyle.FLOW) {
            return insertFlow(text, mapping, content, offsets);
        }
        return insertBlock(text, mapping, content, offsets);
    }

    private Node child(MappingNode mapping, String name) {
        Set<String> names = new HashSet<>();
        for (var entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode key) {
                if (!names.add(key.getValue())) {
                    throw new SourceEditRefusal(
                            SourceEditCode.AMBIGUOUS_LAYOUT,
                            "Duplicate YAML source key: " + key.getValue()
                    );
                }
                if ("<<".equals(key.getValue())) {
                    throw new SourceEditRefusal(
                            SourceEditCode.MERGE_KEY,
                            "Merge-based source mappings cannot be patched"
                    );
                }
            }
        }
        for (var entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode key && name.equals(key.getValue())) {
                Node value = entry.getValueNode();
                if (value.getStartMark().orElseThrow().getIndex() < key.getEndMark().orElseThrow().getIndex()) {
                    throw new SourceEditRefusal(
                            SourceEditCode.SHARED_YAML_NODE,
                            "Alias-based source mappings cannot be patched"
                    );
                }
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> nested(
            DocumentPath parent,
            int existingDepth,
            String name,
            Object value
    ) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put(name, value);
        for (int index = parent.segments().size() - 1; index >= existingDepth; index--) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put(parent.segments().get(index), content);
            content = wrapper;
        }
        return content;
    }

    private String insertFlow(
            String text,
            MappingNode mapping,
            Map<String, Object> content,
            YamlSourceOffsets offsets
    ) {
        int position = offsets.charOffset(mapping.getEndMark().orElseThrow().getIndex()) - 1;
        String serialized = JSON.writeValueAsString(content);
        String member = serialized.substring(1, serialized.length() - 1);
        String separator = ", ";
        if (mapping.getValue().isEmpty()) {
            separator = "";
        }
        return text.substring(0, position) + separator + member + text.substring(position);
    }

    private String insertBlock(
            String text,
            MappingNode mapping,
            Map<String, Object> content,
            YamlSourceOffsets offsets
    ) {
        if (mapping.getValue().isEmpty()) {
            throw new SourceEditRefusal(SourceEditCode.AMBIGUOUS_LAYOUT, "Empty block mapping cannot be patched");
        }
        var last = mapping.getValue().getLast();
        if (last.getValueNode().getStartMark().orElseThrow().getIndex()
                < last.getKeyNode().getEndMark().orElseThrow().getIndex()) {
            throw new SourceEditRefusal(
                    SourceEditCode.SHARED_YAML_NODE,
                    "Alias-based source mappings cannot be patched"
            );
        }
        int lastEnd = lastContentEnd(last.getValueNode(), offsets);
        int position = nextLine(text, lastEnd);
        int indentation = indentation(
                text,
                offsets.charOffset(last.getKeyNode().getStartMark().orElseThrow().getIndex())
        );
        String newline = SourceLineEndings.near(text, position);
        String prefix = "";
        if (position == text.length() && !text.endsWith("\n")) {
            prefix = newline;
        }
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
        if (newline < 0) {
            return text.length();
        }
        return newline + 1;
    }

    private int lastContentEnd(Node node, YamlSourceOffsets offsets) {
        if (node instanceof MappingNode mapping
                && mapping.getFlowStyle() != FlowStyle.FLOW
                && !mapping.getValue().isEmpty()) {
            return lastContentEnd(mapping.getValue().getLast().getValueNode(), offsets);
        }
        if (node instanceof SequenceNode sequence
                && sequence.getFlowStyle() != FlowStyle.FLOW
                && !sequence.getValue().isEmpty()) {
            return lastContentEnd(sequence.getValue().getLast(), offsets);
        }
        return offsets.charOffset(node.getEndMark().orElseThrow().getIndex());
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
        renderMapping(builder, content, indent, newline);
        return builder.toString();
    }

    private void renderMapping(StringBuilder builder, Map<String, Object> content, int indent, String newline) {
        for (var entry : content.entrySet()) {
            builder.append(" ".repeat(indent)).append(SourceTextEdits.yamlScalar(entry.getKey())).append(':');
            if (entry.getValue() instanceof Map<?, ?> children && !children.isEmpty()) {
                builder.append(newline);
                renderMapping(builder, castMap(children), indent + 2, newline);
            } else if (entry.getValue() instanceof List<?> values && containsComposite(values)) {
                builder.append(newline);
                renderSequence(builder, values, indent + 2, newline);
            } else {
                builder.append(' ').append(scalar(entry.getValue())).append(newline);
            }
        }
    }

    private void renderSequence(StringBuilder builder, List<?> values, int indent, String newline) {
        for (Object value : values) {
            builder.append(" ".repeat(indent)).append('-');
            if (value instanceof Map<?, ?> children && !children.isEmpty()) {
                builder.append(newline);
                renderMapping(builder, castMap(children), indent + 2, newline);
            } else if (value instanceof List<?> children && containsComposite(children)) {
                builder.append(newline);
                renderSequence(builder, children, indent + 2, newline);
            } else {
                builder.append(' ').append(scalar(value)).append(newline);
            }
        }
    }

    private boolean containsComposite(List<?> values) {
        return values.stream().anyMatch(value -> value instanceof Map<?, ?> || value instanceof List<?>);
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
