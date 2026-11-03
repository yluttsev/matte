package io.github.yluttsev.matte.openapi.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

final class YamlScalarScanner implements SourceScalarScanner {

    private static final LoadSettings SETTINGS = LoadSettings.builder().build();

    @Override
    public List<SourceScalar> scan(String text) {
        Node root = new Compose(SETTINGS).composeString(text).orElseThrow();
        List<SourceScalar> scalars = new ArrayList<>();
        Set<Node> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        visit(root, DocumentPath.root(), false, scalars, ancestors);
        return scalars;
    }

    private void visit(Node node, DocumentPath path, boolean key, List<SourceScalar> scalars, Set<Node> ancestors) {
        if (!ancestors.add(node)) {
            throw new IllegalArgumentException("Cyclic YAML alias cannot be patched");
        }
        try {
            if (node instanceof ScalarNode scalar) {
                scalars.add(scalar(path, key, scalar));
            } else if (node instanceof MappingNode mapping) {
                visitMapping(mapping, path, scalars, ancestors);
            } else if (node instanceof SequenceNode sequence) {
                visitSequence(sequence, path, scalars, ancestors);
            }
        } finally {
            ancestors.remove(node);
        }
    }

    private void visitMapping(
            MappingNode mapping,
            DocumentPath path,
            List<SourceScalar> scalars,
            Set<Node> ancestors
    ) {
        for (var entry : mapping.getValue()) {
            if (!(entry.getKeyNode() instanceof ScalarNode key)) {
                throw new IllegalArgumentException("Non-scalar YAML key cannot be patched");
            }
            DocumentPath child = path.child(key.getValue());
            visit(key, child, true, scalars, ancestors);
            visit(entry.getValueNode(), child, false, scalars, ancestors);
        }
    }

    private void visitSequence(
            SequenceNode sequence,
            DocumentPath path,
            List<SourceScalar> scalars,
            Set<Node> ancestors
    ) {
        for (int index = 0; index < sequence.getValue().size(); index++) {
            visit(sequence.getValue().get(index), path.child(Integer.toString(index)), false, scalars, ancestors);
        }
    }

    private SourceScalar scalar(DocumentPath path, boolean key, ScalarNode node) {
        int start = node.getStartMark().orElseThrow().getIndex();
        int end = node.getEndMark().orElseThrow().getIndex();
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
