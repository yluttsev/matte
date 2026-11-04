package io.github.yluttsev.matte.openapi.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.yluttsev.matte.core.model.value.ArrayValue;
import io.github.yluttsev.matte.core.model.value.BooleanValue;
import io.github.yluttsev.matte.core.model.value.DocumentValue;
import io.github.yluttsev.matte.core.model.value.NullValue;
import io.github.yluttsev.matte.core.model.value.NumberValue;
import io.github.yluttsev.matte.core.model.value.ObjectValue;
import io.github.yluttsev.matte.core.model.value.StringValue;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.OpenApiVersion;
import io.github.yluttsev.matte.openapi.SourceFormat;
import io.github.yluttsev.matte.openapi.mapping.SchemaValueFields;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class SourceSchemaValueReader {

    private static final String COMPONENTS_KEY = "components";
    private static final String SCHEMAS_KEY = "schemas";
    private static final List<String> COMPOSITION_FIELDS = List.of("allOf", "oneOf", "anyOf");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LoadSettings YAML = LoadSettings.builder().build();

    private SourceSchemaValueReader() {
    }

    public static Map<DocumentPath, SchemaValueFields> read(String text, SourceFormat format, OpenApiVersion version) {
        return format == SourceFormat.JSON ? readJson(text, version) : readYaml(text, version);
    }

    private static Map<DocumentPath, SchemaValueFields> readJson(String text, OpenApiVersion version) {
        JsonNode schemas = JSON.readTree(text).path(COMPONENTS_KEY).path(SCHEMAS_KEY);
        Map<DocumentPath, SchemaValueFields> values = new LinkedHashMap<>();
        for (String name : schemas.propertyNames()) {
            collectJsonSchema(schemas.get(name), DocumentPath.schemaComponent(name), values, version);
        }
        return Map.copyOf(values);
    }

    private static void collectJsonSchema(JsonNode schema, DocumentPath path,
            Map<DocumentPath, SchemaValueFields> values, OpenApiVersion version) {
        if (!schema.isObject()) {
            return;
        }
        values.put(path, new SchemaValueFields(
                schema.has("default"), jsonValue(schema.get("default")),
                schema.has("example"), jsonValue(schema.get("example")),
                version == OpenApiVersion.V3_1 ? jsonValues(schema.get("examples")) : List.of(),
                jsonValues(schema.get("enum")),
                version == OpenApiVersion.V3_1 && schema.has("const"),
                version == OpenApiVersion.V3_1 ? jsonValue(schema.get("const")) : null));

        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            for (String name : properties.propertyNames()) {
                collectJsonSchema(properties.get(name), path.child("properties").child(name), values, version);
            }
        }
        collectJsonChild(schema, "items", path, values, version);
        collectJsonChild(schema, "additionalProperties", path, values, version);
        collectJsonChild(schema, "not", path, values, version);
        for (String field : COMPOSITION_FIELDS) {
            JsonNode branches = schema.get(field);
            if (branches != null && branches.isArray()) {
                for (int index = 0; index < branches.size(); index++) {
                    collectJsonSchema(branches.get(index), path.child(field).child(Integer.toString(index)),
                            values, version);
                }
            }
        }
    }

    private static void collectJsonChild(JsonNode schema, String field, DocumentPath path,
            Map<DocumentPath, SchemaValueFields> values, OpenApiVersion version) {
        JsonNode child = schema.get(field);
        if (child != null) {
            collectJsonSchema(child, path.child(field), values, version);
        }
    }

    private static List<DocumentValue> jsonValues(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<DocumentValue> values = new ArrayList<>();
        for (JsonNode value : node) {
            values.add(jsonValue(value));
        }
        return values;
    }

    private static DocumentValue jsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return new NullValue();
        }
        if (node.isBoolean()) {
            return new BooleanValue(node.asBoolean());
        }
        if (node.isNumber()) {
            return new NumberValue(new BigDecimal(node.asString()));
        }
        if (node.isString()) {
            return new StringValue(node.asString());
        }
        if (node.isArray()) {
            List<DocumentValue> values = new ArrayList<>();
            for (JsonNode value : node) {
                values.add(jsonValue(value));
            }
            return new ArrayValue(values);
        }
        Map<String, DocumentValue> properties = new LinkedHashMap<>();
        for (String name : node.propertyNames()) {
            properties.put(name, jsonValue(node.get(name)));
        }
        return new ObjectValue(properties);
    }

    private static Map<DocumentPath, SchemaValueFields> readYaml(String text, OpenApiVersion version) {
        Node root = new Compose(YAML).composeString(text).orElse(null);
        Node schemas = mappingValue(mappingValue(root, COMPONENTS_KEY), SCHEMAS_KEY);
        Map<DocumentPath, SchemaValueFields> values = new LinkedHashMap<>();
        Set<Node> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        if (schemas instanceof MappingNode mapping) {
            for (NodeTuple tuple : mapping.getValue()) {
                if (tuple.getKeyNode() instanceof ScalarNode key) {
                    collectYamlSchema(tuple.getValueNode(), DocumentPath.schemaComponent(key.getValue()),
                            values, ancestors, version);
                }
            }
        }
        return Map.copyOf(values);
    }

    private static void collectYamlSchema(Node schema, DocumentPath path,
            Map<DocumentPath, SchemaValueFields> values, Set<Node> ancestors, OpenApiVersion version) {
        if (!(schema instanceof MappingNode mapping) || !ancestors.add(schema)) {
            return;
        }
        try {
            Node defaultNode = mappingValue(mapping, "default");
            Node exampleNode = mappingValue(mapping, "example");
            Node constNode = version == OpenApiVersion.V3_1 ? mappingValue(mapping, "const") : null;
            values.put(path, new SchemaValueFields(
                    defaultNode != null, yamlValueOrNull(defaultNode, ancestors),
                    exampleNode != null, yamlValueOrNull(exampleNode, ancestors),
                    version == OpenApiVersion.V3_1 ? yamlValues(mappingValue(mapping, "examples"), ancestors)
                            : List.of(),
                    yamlValues(mappingValue(mapping, "enum"), ancestors),
                    constNode != null, yamlValueOrNull(constNode, ancestors)));

            Node properties = mappingValue(mapping, "properties");
            if (properties instanceof MappingNode propertyMap) {
                for (NodeTuple tuple : propertyMap.getValue()) {
                    if (tuple.getKeyNode() instanceof ScalarNode key) {
                        collectYamlSchema(tuple.getValueNode(), path.child("properties").child(key.getValue()),
                                values, ancestors, version);
                    }
                }
            }
            collectYamlChild(mapping, "items", path, values, ancestors, version);
            collectYamlChild(mapping, "additionalProperties", path, values, ancestors, version);
            collectYamlChild(mapping, "not", path, values, ancestors, version);
            for (String field : COMPOSITION_FIELDS) {
                Node branches = mappingValue(mapping, field);
                if (branches instanceof SequenceNode sequence) {
                    for (int index = 0; index < sequence.getValue().size(); index++) {
                        collectYamlSchema(sequence.getValue().get(index),
                                path.child(field).child(Integer.toString(index)), values, ancestors, version);
                    }
                }
            }
        } finally {
            ancestors.remove(schema);
        }
    }

    private static void collectYamlChild(MappingNode schema, String field, DocumentPath path,
            Map<DocumentPath, SchemaValueFields> values, Set<Node> ancestors, OpenApiVersion version) {
        Node child = mappingValue(schema, field);
        if (child != null) {
            collectYamlSchema(child, path.child(field), values, ancestors, version);
        }
    }

    private static List<DocumentValue> yamlValues(Node node, Set<Node> ancestors) {
        if (!(node instanceof SequenceNode sequence)) {
            return List.of();
        }
        List<DocumentValue> values = new ArrayList<>();
        for (Node value : sequence.getValue()) {
            values.add(yamlValue(value, ancestors));
        }
        return values;
    }

    private static DocumentValue yamlValueOrNull(Node node, Set<Node> ancestors) {
        return node == null ? null : yamlValue(node, ancestors);
    }

    private static DocumentValue yamlValue(Node node, Set<Node> ancestors) {
        if (node instanceof ScalarNode scalar) {
            return yamlScalar(scalar);
        }
        if (!ancestors.add(node)) {
            throw new IllegalArgumentException("Cyclic YAML value cannot be mapped");
        }
        try {
            if (node instanceof SequenceNode sequence) {
                List<DocumentValue> values = new ArrayList<>();
                for (Node value : sequence.getValue()) {
                    values.add(yamlValue(value, ancestors));
                }
                return new ArrayValue(values);
            }
            if (node instanceof MappingNode mapping) {
                Map<String, DocumentValue> values = new LinkedHashMap<>();
                for (NodeTuple tuple : mapping.getValue()) {
                    if (!(tuple.getKeyNode() instanceof ScalarNode key)) {
                        throw new IllegalArgumentException("Schema value object keys must be strings");
                    }
                    values.put(key.getValue(), yamlValue(tuple.getValueNode(), ancestors));
                }
                return new ObjectValue(values);
            }
            throw new IllegalArgumentException("Unsupported schema value node: " + node.getNodeType());
        } finally {
            ancestors.remove(node);
        }
    }

    private static DocumentValue yamlScalar(ScalarNode scalar) {
        String value = scalar.getValue();
        if (Tag.NULL.equals(scalar.getTag())) {
            return new NullValue();
        }
        if (Tag.BOOL.equals(scalar.getTag())) {
            return new BooleanValue(Boolean.parseBoolean(value));
        }
        if (Tag.INT.equals(scalar.getTag()) || Tag.FLOAT.equals(scalar.getTag())) {
            return new NumberValue(new BigDecimal(value.replace("_", "")));
        }
        return new StringValue(value);
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
