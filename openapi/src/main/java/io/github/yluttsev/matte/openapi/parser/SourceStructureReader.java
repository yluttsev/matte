package io.github.yluttsev.matte.openapi.parser;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import io.github.yluttsev.matte.openapi.SourceFormat;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class SourceStructureReader {

    private static final String OPENAPI_KEY = "openapi";
    private static final String SWAGGER_KEY = "swagger";
    private static final String COMPONENTS_KEY = "components";
    private static final String SCHEMAS_KEY = "schemas";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LoadSettings SETTINGS = LoadSettings.builder().build();

    private SourceStructureReader() {
    }

    public static Optional<String> version(String text, SourceFormat format) {
        if (format == SourceFormat.JSON) {
            return jsonVersion(text);
        }
        return yamlVersion(text);
    }

    public static Set<String> schemaNames(String text, SourceFormat format) {
        if (format == SourceFormat.JSON) {
            return jsonSchemaNames(text);
        }
        return yamlSchemaNames(text);
    }

    private static Set<String> jsonSchemaNames(String text) {
        JsonNode schemas = MAPPER.readTree(text).path(COMPONENTS_KEY).path(SCHEMAS_KEY);
        return new LinkedHashSet<>(schemas.propertyNames());
    }

    private static Set<String> yamlSchemaNames(String text) {
        Node root = new Compose(SETTINGS).composeString(text).orElseThrow();
        Node schemas = mappingValue(mappingValue(root, COMPONENTS_KEY), SCHEMAS_KEY);
        Set<String> names = new LinkedHashSet<>();
        if (schemas instanceof MappingNode mapping) {
            for (NodeTuple tuple : mapping.getValue()) {
                if (tuple.getKeyNode() instanceof ScalarNode key) {
                    names.add(key.getValue());
                }
            }
        }
        return names;
    }

    private static Optional<String> jsonVersion(String text) {
        JsonNode root = MAPPER.readTree(text);
        if (!root.isObject()) {
            return Optional.empty();
        }
        return jsonString(root, OPENAPI_KEY).or(() -> jsonString(root, SWAGGER_KEY));
    }

    private static Optional<String> jsonString(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isString()) {
            return Optional.of(value.asString());
        }
        return Optional.empty();
    }

    private static Optional<String> yamlVersion(String text) {
        Node root = new Compose(SETTINGS).composeString(text).orElse(null);
        return scalarValue(root, OPENAPI_KEY).or(() -> scalarValue(root, SWAGGER_KEY));
    }

    private static Optional<String> scalarValue(Node node, String name) {
        if (mappingValue(node, name) instanceof ScalarNode scalar) {
            return Optional.of(scalar.getValue());
        }
        return Optional.empty();
    }

    private static Node mappingValue(Node node, String name) {
        if (node instanceof MappingNode mapping) {
            for (NodeTuple tuple : mapping.getValue()) {
                if (tuple.getKeyNode() instanceof ScalarNode key && name.equals(key.getValue())) {
                    return tuple.getValueNode();
                }
            }
        }
        return null;
    }
}
