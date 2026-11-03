package io.github.yluttsev.matte.openapi.parser;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import io.github.yluttsev.matte.openapi.ImportDiagnostic;
import io.github.yluttsev.matte.openapi.ImportStatus;
import io.github.yluttsev.matte.openapi.SourceLocation;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

public final class YamlSyntaxParser implements SyntaxParser {

    private static final String MULTIPLE_DOCUMENTS_MESSAGE = "YAML must contain exactly one document";
    private static final String DUPLICATE_KEY_PREFIX = "Duplicate YAML key: ";
    private static final LoadSettings SETTINGS = LoadSettings.builder().build();

    @Override
    public Optional<ImportDiagnostic> validate(String text) {
        try {
            Iterator<Node> documents = new Compose(SETTINGS).composeAllFromString(text).iterator();
            if (!documents.hasNext()) {
                return Optional.of(ImportDiagnostic.emptySource());
            }
            Node root = documents.next();
            if (documents.hasNext()) {
                return Optional.of(new ImportDiagnostic(ImportStatus.MULTIPLE_DOCUMENTS, MULTIPLE_DOCUMENTS_MESSAGE));
            }
            Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            return checkDuplicateKeys(root, visited);
        } catch (MarkedYamlEngineException exception) {
            return Optional.of(toDiagnostic(exception));
        } catch (YamlEngineException exception) {
            String message = Objects.requireNonNullElse(exception.getMessage(), "Invalid YAML syntax");
            return Optional.of(new ImportDiagnostic(ImportStatus.SYNTAX_ERROR, message));
        }
    }

    private static ImportDiagnostic toDiagnostic(MarkedYamlEngineException exception) {
        Optional<SourceLocation> location = exception.getProblemMark().map(YamlSyntaxParser::location);
        String message = Objects.requireNonNullElse(exception.getProblem(), exception.getMessage());
        return new ImportDiagnostic(
                ImportStatus.SYNTAX_ERROR,
                Objects.requireNonNullElse(message, "Invalid YAML syntax"),
                location
        );
    }

    private static Optional<ImportDiagnostic> checkDuplicateKeys(Node node, Set<Node> visited) {
        if (!visited.add(node)) {
            return Optional.empty();
        }
        if (node instanceof MappingNode mapping) {
            return checkMapping(mapping, visited);
        }
        if (node instanceof SequenceNode sequence) {
            return checkSequence(sequence, visited);
        }
        return Optional.empty();
    }

    private static Optional<ImportDiagnostic> checkMapping(MappingNode mapping, Set<Node> visited) {
        Set<String> keys = new HashSet<>();
        for (NodeTuple tuple : mapping.getValue()) {
            Node key = tuple.getKeyNode();
            if (key instanceof ScalarNode scalar && !keys.add(scalar.getValue())) {
                return Optional.of(duplicateKeyDiagnostic(scalar));
            }
            Optional<ImportDiagnostic> issue = checkDuplicateKeys(key, visited)
                    .or(() -> checkDuplicateKeys(tuple.getValueNode(), visited));
            if (issue.isPresent()) {
                return issue;
            }
        }
        return Optional.empty();
    }

    private static Optional<ImportDiagnostic> checkSequence(SequenceNode sequence, Set<Node> visited) {
        for (Node child : sequence.getValue()) {
            Optional<ImportDiagnostic> issue = checkDuplicateKeys(child, visited);
            if (issue.isPresent()) {
                return issue;
            }
        }
        return Optional.empty();
    }

    private static ImportDiagnostic duplicateKeyDiagnostic(ScalarNode scalar) {
        return new ImportDiagnostic(
                ImportStatus.DUPLICATE_KEY,
                DUPLICATE_KEY_PREFIX + scalar.getValue(),
                scalar.getStartMark().map(YamlSyntaxParser::location)
        );
    }

    private static SourceLocation location(Mark mark) {
        return new SourceLocation(mark.getLine() + 1, mark.getColumn() + 1);
    }
}
