package io.github.yluttsev.matte.openapi.patch;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.SourceEditCode;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

final class YamlAliasSafety {

    private YamlAliasSafety() {
    }

    static void requireUniqueInsertionPath(Node root, DocumentPath path) {
        requireUnshared(root, path, false, true);
    }

    static void requireUniqueChangedValue(Node root, DocumentPath path) {
        requireUnshared(root, path, true, false);
    }

    private static void requireUnshared(
            Node root,
            DocumentPath path,
            boolean inspectTargetSubtree,
            boolean allowMissingPath
    ) {
        IdentityHashMap<Node, Integer> references = new IdentityHashMap<>();
        Set<Node> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
        countReferences(root, references, expanded);

        Node current = root;
        rejectSharedNode(current, references, path);
        for (String segment : path.segments()) {
            Node next = child(current, segment);
            if (next == null) {
                if (allowMissingPath) {
                    return;
                }
                throw new IllegalArgumentException("Source path was not found: " + path);
            }
            current = next;
            rejectSharedNode(current, references, path);
        }
        Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        if (inspectTargetSubtree && containsSharedNode(current, references, visited)) {
            rejectAlias(path);
        }
    }

    private static void countReferences(
            Node node,
            IdentityHashMap<Node, Integer> references,
            Set<Node> expanded
    ) {
        references.merge(node, 1, Integer::sum);
        if (!expanded.add(node)) {
            return;
        }
        if (node instanceof MappingNode mapping) {
            for (NodeTuple entry : mapping.getValue()) {
                countReferences(entry.getKeyNode(), references, expanded);
                countReferences(entry.getValueNode(), references, expanded);
            }
        } else if (node instanceof SequenceNode sequence) {
            for (Node child : sequence.getValue()) {
                countReferences(child, references, expanded);
            }
        }
    }

    private static boolean containsSharedNode(
            Node node,
            IdentityHashMap<Node, Integer> references,
            Set<Node> visited
    ) {
        if (references.getOrDefault(node, 0) > 1) {
            return true;
        }
        if (!visited.add(node)) {
            return false;
        }
        if (node instanceof MappingNode mapping) {
            for (NodeTuple entry : mapping.getValue()) {
                if (containsSharedNode(entry.getKeyNode(), references, visited)
                        || containsSharedNode(entry.getValueNode(), references, visited)) {
                    return true;
                }
            }
        } else if (node instanceof SequenceNode sequence) {
            for (Node child : sequence.getValue()) {
                if (containsSharedNode(child, references, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Node child(Node parent, String segment) {
        if (parent instanceof MappingNode mapping) {
            for (NodeTuple entry : mapping.getValue()) {
                if (entry.getKeyNode() instanceof ScalarNode key && segment.equals(key.getValue())) {
                    return entry.getValueNode();
                }
            }
        } else if (parent instanceof SequenceNode sequence) {
            int index;
            try {
                index = Integer.parseInt(segment);
            } catch (NumberFormatException exception) {
                return null;
            }
            if (index >= 0 && index < sequence.getValue().size()) {
                return sequence.getValue().get(index);
            }
        }
        return null;
    }

    private static void rejectSharedNode(
            Node node,
            IdentityHashMap<Node, Integer> references,
            DocumentPath path
    ) {
        if (references.getOrDefault(node, 0) > 1) {
            rejectAlias(path);
        }
    }

    private static void rejectAlias(DocumentPath path) {
        throw new SourceEditRefusal(
                SourceEditCode.SHARED_YAML_NODE,
                "YAML aliases make source edits ambiguous at: " + path,
                path
        );
    }
}
