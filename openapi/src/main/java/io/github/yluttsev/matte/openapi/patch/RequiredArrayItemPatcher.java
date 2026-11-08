package io.github.yluttsev.matte.openapi.patch;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceEditCode;
import io.github.yluttsev.matte.openapi.SourceFormat;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class RequiredArrayItemPatcher {

    private static final char UTF8_BOM = 0xFEFF;
    private static final String DEFAULT_SEPARATOR = ", ";
    private static final Pattern ITEM_SEPARATOR = Pattern.compile(",\\s*");
    private static final Pattern BLOCK_ITEM_PREFIX = Pattern.compile(" *-[ ]+");
    private static final LoadSettings YAML_SETTINGS = LoadSettings.builder().build();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<SourceFormat, SourceScalarScanner> SCANNERS = Map.of(
            SourceFormat.JSON,
            new JsonScalarScanner(),
            SourceFormat.YAML,
            new YamlScalarScanner()
    );

    String applyDifference(
            String text,
            SourceFormat format,
            DocumentPath sequencePath,
            Collection<String> removedNames,
            Collection<String> addedNames
    ) {
        requireUnshared(text, format, sequencePath);
        String current = text;
        for (String name : addedNames) {
            current = append(current, format, sequencePath, name);
        }
        if (!removedNames.isEmpty()) {
            current = removeNames(current, format, sequencePath, removedNames);
        }
        return current;
    }

    private void requireUnshared(String text, SourceFormat format, DocumentPath sequencePath) {
        if (format == SourceFormat.YAML) {
            YamlAliasSafety.requireUniqueChangedValue(
                    new Compose(YAML_SETTINGS).composeString(text).orElseThrow(),
                    sequencePath
            );
        }
    }

    private String removeNames(
            String text,
            SourceFormat format,
            DocumentPath sequencePath,
            Collection<String> names
    ) {
        List<SourceScalar> items = stringItems(text, format, sequencePath);
        List<SourceScalar> itemsToRemove = items.stream()
                .filter(item -> names.contains(item.value()))
                .toList();
        if (itemsToRemove.isEmpty()) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required array entry could not be located: " + sequencePath,
                    sequencePath
            );
        }
        SourceTextEdits edits = new SourceTextEdits();
        remove(text, format, sequencePath, items, itemsToRemove, edits);
        return edits.apply(text);
    }

    private String append(String text, SourceFormat format, DocumentPath sequencePath, String name) {
        List<SourceScalar> items = stringItems(text, format, sequencePath);
        if (items.stream().anyMatch(item -> item.value().equals(name))) {
            return text;
        }
        if (format == SourceFormat.JSON) {
            return appendDelimited(text, items, JSON.writeValueAsString(name));
        }
        String value = SourceTextEdits.yamlScalar(name);
        if (sequence(text, sequencePath).getFlowStyle() == FlowStyle.FLOW) {
            return appendDelimited(text, items, value);
        }
        return appendYamlBlock(text, items, value);
    }

    private List<SourceScalar> stringItems(String text, SourceFormat format, DocumentPath sequencePath) {
        List<SourceScalar> items = SCANNERS.get(format).scan(text).stream()
                .filter(scalar -> !scalar.key())
                .filter(scalar -> scalar.path().parent().filter(sequencePath::equals).isPresent())
                .sorted(Comparator.comparingInt(this::sequenceIndex))
                .toList();
        if (items.isEmpty()) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required array entry could not be located: " + sequencePath,
                    sequencePath
            );
        }
        validateYamlIndexes(items);
        if (sourceItemCount(text, format, sequencePath) != items.size()) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required array contains a non-string entry: " + sequencePath,
                    sequencePath
            );
        }
        return items;
    }

    private int sourceItemCount(String text, SourceFormat format, DocumentPath sequencePath) {
        if (format == SourceFormat.YAML) {
            return sequence(text, sequencePath).getValue().size();
        }
        String parseable = text;
        if (!text.isEmpty() && text.charAt(0) == UTF8_BOM) {
            parseable = text.substring(1);
        }
        JsonNode current = JSON.readTree(parseable);
        for (String segment : sequencePath.segments()) {
            if (current.isArray()) {
                current = current.get(parseIndex(segment, sequencePath));
            } else {
                current = current.get(segment);
            }
            if (current == null) {
                throw new SourceEditRefusal(
                        SourceEditCode.AMBIGUOUS_LAYOUT,
                        "Required array was not found: " + sequencePath,
                        sequencePath
                );
            }
        }
        if (!current.isArray()) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required value is not an array: " + sequencePath,
                    sequencePath
            );
        }
        return current.size();
    }

    private String appendDelimited(String text, List<SourceScalar> items, String value) {
        SourceScalar last = items.getLast();
        String separator = DEFAULT_SEPARATOR;
        if (items.size() > 1) {
            String between = text.substring(items.get(items.size() - 2).end(), last.start());
            if (ITEM_SEPARATOR.matcher(between).matches()) {
                separator = between;
            }
        }
        return text.substring(0, last.end()) + separator + value + text.substring(last.end());
    }

    private String appendYamlBlock(String text, List<SourceScalar> items, String value) {
        SourceScalar last = items.getLast();
        int lineStart = lineStart(text, last.start());
        String marker = text.substring(lineStart, last.start());
        if (!BLOCK_ITEM_PREFIX.matcher(marker).matches()) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required YAML array entry layout cannot be extended safely"
            );
        }
        int position = lineEnd(text, last.end());
        String newline = SourceLineEndings.near(text, lineStart);
        String prefix = "";
        if (position == text.length() && !text.endsWith("\n")) {
            prefix = newline;
        }
        return text.substring(0, position) + prefix + marker + value + newline + text.substring(position);
    }

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
                throw new SourceEditRefusal(
                        SourceEditCode.AMBIGUOUS_LAYOUT,
                        "Required array entry could not be located: " + item.path(),
                        item.path()
                );
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
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required YAML array contains a non-string entry: " + sequencePath,
                    sequencePath
            );
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
                throw new SourceEditRefusal(
                        SourceEditCode.AMBIGUOUS_LAYOUT,
                        "Required YAML array entry could not be located: " + item.path(),
                        item.path()
                );
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
                throw new SourceEditRefusal(
                        SourceEditCode.AMBIGUOUS_LAYOUT,
                        "Required YAML array was not found: " + path
                );
            }
        }
        if (!(current instanceof SequenceNode sequence)) {
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_LAYOUT,
                    "Required YAML value is not an array: " + path
            );
        }
        return sequence;
    }

    private Node mappingChild(MappingNode mapping, String name, DocumentPath path) {
        for (NodeTuple entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode key && name.equals(key.getValue())) {
                return entry.getValueNode();
            }
        }
        throw new SourceEditRefusal(SourceEditCode.AMBIGUOUS_LAYOUT, "Required YAML array was not found: " + path);
    }

    private Node sequenceChild(SequenceNode sequence, String segment, DocumentPath path) {
        int index = parseIndex(segment, path);
        if (index < 0 || index >= sequence.getValue().size()) {
            throw new SourceEditRefusal(SourceEditCode.AMBIGUOUS_LAYOUT, "Required YAML array was not found: " + path);
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
            throw new SourceEditRefusal(
                    SourceEditCode.AMBIGUOUS_COMMENT,
                    "Comments around a required array entry cannot be moved safely"
            );
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
