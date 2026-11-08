package io.github.yluttsev.matte.openapi.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceEditCode;
import io.github.yluttsev.matte.openapi.parser.ParserSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

final class YamlScalarScanner implements SourceScalarScanner {

    private static final LoadSettings SETTINGS = ParserSettings.yaml();

    @Override
    public List<SourceScalar> scan(String text) {
        Node root = new Compose(SETTINGS).composeString(text).orElseThrow();
        List<SourceScalar> scalars = new ArrayList<>();
        Set<Node> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        YamlSourceOffsets offsets = new YamlSourceOffsets(text);
        visit(root, DocumentPath.root(), false, scalars, ancestors, visited, offsets);
        return scalars;
    }

    private void visit(
            Node node,
            DocumentPath path,
            boolean key,
            List<SourceScalar> scalars,
            Set<Node> ancestors,
            Set<Node> visited,
            YamlSourceOffsets offsets
    ) {
        if (!ancestors.add(node)) {
            throw new SourceEditRefusal(SourceEditCode.SHARED_YAML_NODE, "Cyclic YAML alias cannot be patched", path);
        }
        try {
            if (!visited.add(node)) {
                return;
            }
            if (node instanceof ScalarNode scalar) {
                scalars.add(scalar(path, key, scalar, offsets));
            } else if (node instanceof MappingNode mapping) {
                visitMapping(mapping, path, scalars, ancestors, visited, offsets);
            } else if (node instanceof SequenceNode sequence) {
                visitSequence(sequence, path, scalars, ancestors, visited, offsets);
            }
        } finally {
            ancestors.remove(node);
        }
    }

    private void visitMapping(
            MappingNode mapping,
            DocumentPath path,
            List<SourceScalar> scalars,
            Set<Node> ancestors,
            Set<Node> visited,
            YamlSourceOffsets offsets
    ) {
        Set<String> names = new HashSet<>();
        for (var entry : mapping.getValue()) {
            if (!(entry.getKeyNode() instanceof ScalarNode key)) {
                throw new SourceEditRefusal(
                        SourceEditCode.AMBIGUOUS_LAYOUT,
                        "Non-scalar YAML key cannot be patched",
                        path
                );
            }
            if (!names.add(key.getValue())) {
                throw new SourceEditRefusal(
                        SourceEditCode.AMBIGUOUS_LAYOUT,
                        "Duplicate YAML source key: " + key.getValue(),
                        path
                );
            }
            DocumentPath child = path.child(key.getValue());
            visit(key, child, true, scalars, ancestors, visited, offsets);
            visit(entry.getValueNode(), child, false, scalars, ancestors, visited, offsets);
        }
    }

    private void visitSequence(
            SequenceNode sequence,
            DocumentPath path,
            List<SourceScalar> scalars,
            Set<Node> ancestors,
            Set<Node> visited,
            YamlSourceOffsets offsets
    ) {
        for (int index = 0; index < sequence.getValue().size(); index++) {
            visit(
                    sequence.getValue().get(index),
                    path.child(Integer.toString(index)),
                    false,
                    scalars,
                    ancestors,
                    visited,
                    offsets
            );
        }
    }

    private SourceScalar scalar(DocumentPath path, boolean key, ScalarNode node, YamlSourceOffsets offsets) {
        int start = offsets.charOffset(node.getStartMark().orElseThrow().getIndex());
        int end = offsets.charOffset(node.getEndMark().orElseThrow().getIndex());
        return new SourceScalar(path, key, node.getValue(), start, end, quoteStyle(node.getScalarStyle()));
    }

    private static SourceScalar.QuoteStyle quoteStyle(ScalarStyle style) {
        if (style == ScalarStyle.SINGLE_QUOTED) {
            return SourceScalar.QuoteStyle.SINGLE_QUOTED;
        }
        if (style == ScalarStyle.DOUBLE_QUOTED) {
            return SourceScalar.QuoteStyle.DOUBLE_QUOTED;
        }
        return SourceScalar.QuoteStyle.PLAIN;
    }
}
