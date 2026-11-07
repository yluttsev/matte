package io.github.yluttsev.matte.openapi.patch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import tools.jackson.databind.ObjectMapper;

final class YamlValueSourceEditor implements SchemaValueSourceEditor {

    private static final LoadSettings SETTINGS = LoadSettings.builder().build();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String replace(String text, DocumentPath path, Object value) {
        LocatedNode located = locate(text, path);
        Range range = range(located.node(), located.offsets());
        return text.substring(0, range.start()) + JSON.writeValueAsString(value) + text.substring(range.end());
    }

    @Override
    public String remove(String text, DocumentPath path) {
        LocatedNode located = locate(text, path);
        if (located.tuple() == null || located.parent() == null) {
            throw new IllegalArgumentException("Source field is not part of a mapping: " + path);
        }
        if (located.parent().getValue().size() == 1) {
            return removeOnlyField(text, located, path);
        }
        if (located.parent().getFlowStyle() == FlowStyle.FLOW) {
            return removeFlowField(text, located);
        }
        return removeBlockField(text, located);
    }

    private LocatedNode locate(String text, DocumentPath path) {
        Node current = new Compose(SETTINGS).composeString(text).orElseThrow();
        YamlSourceOffsets offsets = new YamlSourceOffsets(text);
        MappingNode parent = null;
        NodeTuple tuple = null;
        int tupleIndex = -1;
        for (String segment : path.segments()) {
            if (current instanceof MappingNode mapping) {
                tupleIndex = tupleIndex(mapping, segment);
                tuple = mapping.getValue().get(tupleIndex);
                Node key = tuple.getKeyNode();
                Node value = tuple.getValueNode();
                if (value.getStartMark().orElseThrow().getIndex() < key.getEndMark().orElseThrow().getIndex()) {
                    throw new IllegalArgumentException("Alias-based source values cannot be patched: " + path);
                }
                parent = mapping;
                current = value;
            } else if (current instanceof SequenceNode sequence) {
                current = sequenceItem(sequence, segment, path);
                parent = null;
                tuple = null;
                tupleIndex = -1;
            } else {
                throw new IllegalArgumentException("Source path does not point to a value: " + path);
            }
        }
        return new LocatedNode(current, parent, tuple, tupleIndex, offsets);
    }

    private int tupleIndex(MappingNode mapping, String name) {
        int found = -1;
        Set<String> names = new HashSet<>();
        List<NodeTuple> entries = mapping.getValue();
        for (int index = 0; index < entries.size(); index++) {
            Node keyNode = entries.get(index).getKeyNode();
            if (!(keyNode instanceof ScalarNode key)) {
                throw new IllegalArgumentException("Non-scalar YAML mapping keys cannot be patched");
            }
            if (!names.add(key.getValue())) {
                throw new IllegalArgumentException("Duplicate YAML source key: " + key.getValue());
            }
            if ("<<".equals(key.getValue())) {
                throw new IllegalArgumentException("Merge-based source mappings cannot be patched");
            }
            if (name.equals(key.getValue())) {
                found = index;
            }
        }
        if (found < 0) {
            throw new IllegalArgumentException("Source field was not found: " + name);
        }
        return found;
    }

    private Node sequenceItem(SequenceNode sequence, String segment, DocumentPath path) {
        int index;
        try {
            index = Integer.parseInt(segment);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Source path has an invalid sequence index: " + path, exception);
        }
        if (index < 0 || index >= sequence.getValue().size()) {
            throw new IllegalArgumentException("Source sequence item was not found: " + path);
        }
        return sequence.getValue().get(index);
    }

    private String removeOnlyField(String text, LocatedNode located, DocumentPath path) {
        Range parentRange = range(located.parent(), located.offsets());
        int keyStart = charOffset(
                located.tuple().getKeyNode().getStartMark().orElseThrow().getIndex(),
                located.offsets()
        );
        int valueEnd = range(located.node(), located.offsets()).end();
        ensureNoInlineComment(text, valueEnd);
        if (located.parent().getFlowStyle() == FlowStyle.FLOW) {
            ensureNoComment(text, parentRange.start(), keyStart);
            ensureNoComment(text, valueEnd, parentRange.end());
        } else {
            ensureNoLeadingComment(text, keyStart);
        }
        if (located.parent().getFlowStyle() == FlowStyle.FLOW) {
            return text.substring(0, parentRange.start()) + "{}" + text.substring(parentRange.end());
        }
        int end = parentRange.end();
        if (end == text.length()) {
            return text.substring(0, parentRange.start()) + "{}" + trailingLineEndings(text);
        }
        int nextLineStart = lineStart(text, end);
        if (!text.substring(nextLineStart, end).isBlank() || nextLineStart == 0) {
            throw new IllegalArgumentException("YAML block mapping end is ambiguous: " + path);
        }
        int lineBreakStart = blockSeparatorStart(text, parentRange.start(), nextLineStart, path);
        return text.substring(0, parentRange.start()) + "{}" + text.substring(lineBreakStart);
    }

    private int blockSeparatorStart(String text, int start, int nextLineStart, DocumentPath path) {
        int position = nextLineStart;
        while (position > start) {
            int previousLineStart = lineStart(text, position - 1);
            String line = text.substring(previousLineStart, position).strip();
            if (line.startsWith("#")) {
                throw new IllegalArgumentException("Comments around a YAML field cannot be moved safely: " + path);
            }
            if (!line.isEmpty()) {
                int lineBreakStart = position - 1;
                if (lineBreakStart > 0 && text.charAt(lineBreakStart - 1) == '\r') {
                    lineBreakStart--;
                }
                return lineBreakStart;
            }
            position = previousLineStart;
        }
        throw new IllegalArgumentException("YAML block mapping end is ambiguous: " + path);
    }

    private String removeFlowField(String text, LocatedNode located) {
        List<NodeTuple> entries = located.parent().getValue();
        int keyStart = charOffset(
                located.tuple().getKeyNode().getStartMark().orElseThrow().getIndex(),
                located.offsets()
        );
        int valueEnd = charOffset(
                located.tuple().getValueNode().getEndMark().orElseThrow().getIndex(),
                located.offsets()
        );
        int separatorStart;
        int separatorEnd;
        Range mappingRange = range(located.parent(), located.offsets());
        if (located.tupleIndex() > 0) {
            int previousEnd = charOffset(
                    entries.get(located.tupleIndex() - 1).getValueNode().getEndMark().orElseThrow().getIndex(),
                    located.offsets()
            );
            ensureNoComment(text, previousEnd, keyStart);
        } else {
            ensureNoComment(text, mappingRange.start(), keyStart);
        }
        if (located.tupleIndex() < entries.size() - 1) {
            separatorStart = valueEnd;
            separatorEnd = charOffset(
                    entries.get(located.tupleIndex() + 1).getKeyNode().getStartMark().orElseThrow().getIndex(),
                    located.offsets()
            );
            ensureNoComment(text, separatorStart, separatorEnd);
        } else {
            separatorStart = charOffset(
                    entries.get(located.tupleIndex() - 1).getValueNode().getEndMark().orElseThrow().getIndex(),
                    located.offsets()
            );
            separatorEnd = keyStart;
            ensureNoComment(text, valueEnd, mappingRange.end());
        }
        int comma = commaBetween(text, separatorStart, separatorEnd);
        ensureNoInlineComment(text, valueEnd);
        if (located.tupleIndex() < entries.size() - 1) {
            return text.substring(0, keyStart) + text.substring(comma + 1);
        }
        return text.substring(0, comma) + text.substring(valueEnd);
    }

    private String removeBlockField(String text, LocatedNode located) {
        Range value = range(located.node(), located.offsets());
        ensureNoInlineComment(text, value.end());
        int keyStart = charOffset(
                located.tuple().getKeyNode().getStartMark().orElseThrow().getIndex(),
                located.offsets()
        );
        int start = lineStart(text, keyStart);
        int end = removeEnd(text, value.end(), keyStart - start);
        ensureNoLeadingComment(text, keyStart);
        List<NodeTuple> entries = located.parent().getValue();
        if (located.tupleIndex() < entries.size() - 1) {
            int nextKey = charOffset(
                    entries.get(located.tupleIndex() + 1).getKeyNode().getStartMark().orElseThrow().getIndex(),
                    located.offsets()
            );
            ensureNoStandaloneComment(text, end, nextKey);
        }
        return text.substring(0, start) + text.substring(end);
    }

    private int removeEnd(
            String text,
            int valueEnd,
            int keyIndentation
    ) {
        int valueEndLineStart = lineStart(text, valueEnd);
        boolean endsAtNextKey = valueEnd < text.length()
                && text.substring(valueEndLineStart, valueEnd).isBlank()
                && valueEnd - valueEndLineStart <= keyIndentation
                && text.charAt(valueEnd) != '\n'
                && text.charAt(valueEnd) != '\r';
        if (endsAtNextKey) {
            return valueEndLineStart;
        }
        return lineEnd(text, valueEnd);
    }

    private Range range(Node node, YamlSourceOffsets offsets) {
        int start = charOffset(node.getStartMark().orElseThrow().getIndex(), offsets);
        int end = charOffset(node.getEndMark().orElseThrow().getIndex(), offsets);
        return new Range(start, end);
    }

    private int charOffset(int yamlIndex, YamlSourceOffsets offsets) {
        return offsets.charOffset(yamlIndex);
    }

    private int commaBetween(String text, int start, int end) {
        int comma = text.indexOf(',', start);
        if (comma < 0 || comma >= end) {
            throw new IllegalArgumentException("YAML flow mapping separator was not found");
        }
        return comma;
    }

    private void ensureNoComment(String text, int start, int end) {
        if (text.substring(start, end).contains("#")) {
            throw new IllegalArgumentException("Comments around a YAML flow field cannot be moved safely");
        }
    }

    private void ensureNoInlineComment(String text, int valueEnd) {
        int end = lineEnd(text, valueEnd);
        if (text.substring(valueEnd, end).contains("#")) {
            throw new IllegalArgumentException("Inline comments around a YAML field cannot be moved safely");
        }
    }

    private void ensureNoLeadingComment(String text, int keyStart) {
        int keyLineStart = lineStart(text, keyStart);
        int keyIndentation = keyStart - keyLineStart;
        int previousLineEnd = keyLineStart;
        while (previousLineEnd > 0) {
            int previousLineStart = lineStart(text, previousLineEnd - 1);
            String previousLine = text.substring(previousLineStart, previousLineEnd).stripTrailing();
            String content = previousLine.stripLeading();
            if (content.isEmpty()) {
                previousLineEnd = previousLineStart;
                continue;
            }
            int indentation = previousLine.length() - content.length();
            if (content.startsWith("#") && indentation >= keyIndentation) {
                throw new IllegalArgumentException("Leading comments around a YAML field cannot be moved safely");
            }
            return;
        }
    }

    private void ensureNoStandaloneComment(String text, int start, int end) {
        int lineStart = lineStart(text, start);
        while (lineStart < end) {
            int lineEnd = lineEnd(text, lineStart);
            String line = text.substring(lineStart, Math.min(lineEnd, end)).stripLeading();
            if (line.startsWith("#")) {
                throw new IllegalArgumentException("Comments around a YAML field cannot be moved safely");
            }
            lineStart = lineEnd;
        }
    }

    private int lineStart(String text, int position) {
        return text.lastIndexOf('\n', position - 1) + 1;
    }

    private int lineEnd(String text, int position) {
        int newline = text.indexOf('\n', position);
        if (newline < 0) {
            return text.length();
        }
        return newline + 1;
    }

    private String trailingLineEndings(String text) {
        int start = text.length();
        while (start > 0) {
            char character = text.charAt(start - 1);
            if (character != '\n' && character != '\r') {
                break;
            }
            start--;
        }
        return text.substring(start);
    }

    private record Range(int start, int end) {
    }

    private record LocatedNode(Node node, MappingNode parent, NodeTuple tuple, int tupleIndex,
            YamlSourceOffsets offsets) {
    }
}
