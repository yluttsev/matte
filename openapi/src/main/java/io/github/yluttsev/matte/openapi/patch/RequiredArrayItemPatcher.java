package io.github.yluttsev.matte.openapi.patch;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceFormat;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

final class RequiredArrayItemPatcher {

    private static final LoadSettings YAML_SETTINGS = LoadSettings.builder().build();

    Set<DocumentPath> remove(
            String text,
            SourceFormat format,
            DocumentPath sequencePath,
            List<SourceScalar> sequenceItems,
            List<SourceScalar> itemsToRemove,
            SourceTextEdits edits
    ) {
        boolean[] removed = removalFlags(sequenceItems, itemsToRemove);
        if (format == SourceFormat.JSON) {
            removeDelimitedItems(sequenceItems, removed, edits);
        } else {
            removeYamlItems(text, sequencePath, sequenceItems, removed, edits);
        }
        return removedPaths(sequenceItems, removed);
    }

    private boolean[] removalFlags(List<SourceScalar> sequenceItems, List<SourceScalar> itemsToRemove) {
        boolean[] removed = new boolean[sequenceItems.size()];
        for (SourceScalar item : itemsToRemove) {
            int index = sequenceItems.indexOf(item);
            if (index < 0) {
                throw new IllegalArgumentException("Required array entry could not be located: " + item.path());
            }
            removed[index] = true;
        }
        return removed;
    }

    private void removeYamlItems(
            String text,
            DocumentPath sequencePath,
            List<SourceScalar> sequenceItems,
            boolean[] removed,
            SourceTextEdits edits
    ) {
        SequenceNode sequence = sequence(text, sequencePath);
        if (sequence.getValue().size() != sequenceItems.size()) {
            throw new IllegalArgumentException("Required YAML array contains a non-string entry: " + sequencePath);
        }
        validateYamlIndexes(sequenceItems);
        if (sequence.getFlowStyle() == FlowStyle.FLOW) {
            removeYamlFlowItems(text, sequence, sequenceItems, removed, edits);
        } else {
            removeYamlBlockItems(text, sequenceItems, removed, edits);
        }
    }

    private void validateYamlIndexes(List<SourceScalar> sequenceItems) {
        for (int index = 0; index < sequenceItems.size(); index++) {
            SourceScalar item = sequenceItems.get(index);
            if (sequenceIndex(item) != index) {
                throw new IllegalArgumentException("Required YAML array entry could not be located: " + item.path());
            }
        }
    }

    private void removeYamlFlowItems(
            String text,
            SequenceNode sequence,
            List<SourceScalar> sequenceItems,
            boolean[] removed,
            SourceTextEdits edits
    ) {
        YamlSourceOffsets offsets = new YamlSourceOffsets(text);
        int sequenceStart = offsets.charOffset(sequence.getStartMark().orElseThrow().getIndex());
        int sequenceEnd = offsets.charOffset(sequence.getEndMark().orElseThrow().getIndex());
        for (int index = 0; index < sequenceItems.size(); index++) {
            if (!removed[index]) {
                continue;
            }
            SourceScalar item = sequenceItems.get(index);
            int previousEnd = sequenceStart;
            if (index > 0) {
                previousEnd = sequenceItems.get(index - 1).end();
            }
            ensureNoComment(text, previousEnd, item.start());
            int nextStart = sequenceEnd;
            if (index < sequenceItems.size() - 1) {
                nextStart = sequenceItems.get(index + 1).start();
            }
            ensureNoComment(text, item.end(), nextStart);
        }
        removeDelimitedItems(sequenceItems, removed, edits);
    }

    private void removeDelimitedItems(
            List<SourceScalar> sequenceItems,
            boolean[] removed,
            SourceTextEdits edits
    ) {
        int index = 0;
        while (index < removed.length) {
            if (!removed[index]) {
                index++;
                continue;
            }
            int first = index;
            while (index + 1 < removed.length && removed[index + 1]) {
                index++;
            }
            int last = index;
            int start;
            int end;
            if (first == 0 && last == sequenceItems.size() - 1) {
                start = sequenceItems.get(first).start();
                end = sequenceItems.get(last).end();
            } else if (last < sequenceItems.size() - 1) {
                start = sequenceItems.get(first).start();
                end = sequenceItems.get(last + 1).start();
            } else {
                start = sequenceItems.get(first - 1).end();
                end = sequenceItems.get(last).end();
            }
            edits.remove(start, end);
            index++;
        }
    }

    private void removeYamlBlockItems(
            String text,
            List<SourceScalar> sequenceItems,
            boolean[] removed,
            SourceTextEdits edits
    ) {
        for (int index = 0; index < sequenceItems.size(); index++) {
            if (!removed[index]) {
                continue;
            }
            SourceScalar item = sequenceItems.get(index);
            int start = lineStart(text, item.start());
            int end = lineEnd(text, item.end());
            ensureNoComment(text, item.end(), end);
            if (index > 0) {
                int previousEnd = lineEnd(text, sequenceItems.get(index - 1).end());
                ensureNoComment(text, previousEnd, start);
            }
            if (index < sequenceItems.size() - 1) {
                int nextStart = lineStart(text, sequenceItems.get(index + 1).start());
                ensureNoComment(text, end, nextStart);
            }
            edits.remove(start, end);
        }
    }

    private Set<DocumentPath> removedPaths(List<SourceScalar> sequenceItems, boolean[] removed) {
        Set<DocumentPath> paths = new LinkedHashSet<>();
        for (int index = 0; index < removed.length; index++) {
            if (removed[index]) {
                paths.add(sequenceItems.get(index).path());
            }
        }
        return Set.copyOf(paths);
    }

    private SequenceNode sequence(String text, DocumentPath path) {
        Node current = new Compose(YAML_SETTINGS).composeString(text).orElseThrow();
        for (String segment : path.segments()) {
            if (current instanceof MappingNode mapping) {
                current = mappingChild(mapping, segment, path);
            } else if (current instanceof SequenceNode sequence) {
                current = sequenceChild(sequence, segment, path);
            } else {
                throw new IllegalArgumentException("Required YAML array was not found: " + path);
            }
        }
        if (!(current instanceof SequenceNode sequence)) {
            throw new IllegalArgumentException("Required YAML value is not an array: " + path);
        }
        return sequence;
    }

    private Node mappingChild(MappingNode mapping, String name, DocumentPath path) {
        for (NodeTuple entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode key && name.equals(key.getValue())) {
                return entry.getValueNode();
            }
        }
        throw new IllegalArgumentException("Required YAML array was not found: " + path);
    }

    private Node sequenceChild(SequenceNode sequence, String segment, DocumentPath path) {
        int index = parseIndex(segment, path);
        if (index < 0 || index >= sequence.getValue().size()) {
            throw new IllegalArgumentException("Required YAML array was not found: " + path);
        }
        return sequence.getValue().get(index);
    }

    private int sequenceIndex(SourceScalar scalar) {
        return parseIndex(scalar.path().segments().getLast(), scalar.path());
    }

    private int parseIndex(String segment, DocumentPath path) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Required entry does not have a sequence index: " + path, exception);
        }
    }

    private void ensureNoComment(String text, int start, int end) {
        if (text.substring(start, end).contains("#")) {
            throw new IllegalArgumentException("Comments around a required array entry cannot be moved safely");
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
}
