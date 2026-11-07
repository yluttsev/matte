package io.github.yluttsev.matte.openapi.patch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

final class SchemaReferenceRelocator {

    private static final String REF_FIELD = "$ref";
    private static final String ID_FIELD = "$id";
    private static final String COMPONENTS_FIELD = "components";
    private static final String SCHEMAS_FIELD = "schemas";
    private static final String DISCRIMINATOR_FIELD = "discriminator";
    private static final String MAPPING_FIELD = "mapping";
    private static final Set<String> LITERAL_SCHEMA_FIELDS = Set.of("default", "enum", "const", "example");
    private static final Set<String> NAMED_MAP_FIELDS = Set.of(
            "schemas",
            "properties",
            "patternProperties",
            "dependentSchemas",
            "definitions",
            "$defs",
            "examples",
            "headers",
            "parameters",
            "responses",
            "requestBodies",
            "callbacks",
            "links",
            "securitySchemes",
            "content",
            "encoding",
            "mapping"
    );
    private static final Set<String> COMPONENT_REFERENCE_FIELDS = Set.of(
            "callbacks",
            "examples",
            "headers",
            "links",
            "parameters",
            "requestBodies",
            "responses"
    );
    private static final Set<String> SINGLE_SCHEMA_FIELDS = Set.of(
            "additionalProperties",
            "contains",
            "else",
            "if",
            "items",
            "not",
            "propertyNames",
            "then",
            "unevaluatedItems",
            "unevaluatedProperties"
    );
    private static final Set<String> MAP_SCHEMA_FIELDS = Set.of(
            "$defs",
            "definitions",
            "dependentSchemas",
            "patternProperties",
            "properties"
    );
    private static final Set<String> ARRAY_SCHEMA_FIELDS = Set.of("allOf", "anyOf", "oneOf", "prefixItems");
    private static final Set<String> HTTP_METHODS = Set.of(
            "get",
            "put",
            "post",
            "delete",
            "options",
            "head",
            "patch",
            "trace"
    );

    private final String oldMappingName;
    private final String newMappingName;
    private final DocumentPath oldPath;
    private final DocumentPath newPath;
    private final DocumentPath requiredPath;
    private final String oldRequiredName;
    private final String newRequiredName;

    SchemaReferenceRelocator(String oldName, String newName) {
        this(
                DocumentPath.schemaComponent(oldName),
                DocumentPath.schemaComponent(newName),
                oldName,
                newName,
                null,
                null,
                null
        );
    }

    SchemaReferenceRelocator(
            DocumentPath oldPath,
            DocumentPath newPath,
            DocumentPath requiredPath,
            String oldRequiredName,
            String newRequiredName
    ) {
        this(oldPath, newPath, null, null, requiredPath, oldRequiredName, newRequiredName);
    }

    SchemaReferenceRelocator(
            DocumentPath oldPath,
            DocumentPath newPath
    ) {
        this(oldPath, newPath, null, null, null, null, null);
    }

    private SchemaReferenceRelocator(
            DocumentPath oldPath,
            DocumentPath newPath,
            String oldMappingName,
            String newMappingName,
            DocumentPath requiredPath,
            String oldRequiredName,
            String newRequiredName
    ) {
        this.oldMappingName = oldMappingName;
        this.newMappingName = newMappingName;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.requiredPath = requiredPath;
        this.oldRequiredName = oldRequiredName;
        this.newRequiredName = newRequiredName;
    }

    DocumentPath oldPath() {
        return oldPath;
    }

    String updatedValue(SourceScalar scalar) {
        if (scalar.key()) {
            return scalar.value();
        }
        List<String> segments = scalar.path().segments();
        if (isOpaqueValue(segments)) {
            return scalar.value();
        }
        if (isDiscriminatorMapping(segments)) {
            if (oldMappingName != null && scalar.value().equals(oldMappingName)) {
                return newMappingName;
            }
            return relocateReference(scalar.value());
        }
        if (!segments.isEmpty() && REF_FIELD.equals(segments.getLast())) {
            if (isReferenceLocation(segments)) {
                return relocateReference(scalar.value());
            }
            return scalar.value();
        }
        if (isRequiredValue(scalar) && scalar.value().equals(oldRequiredName)) {
            return newRequiredName;
        }
        return scalar.value();
    }

    boolean referencesTarget(
            SourceScalar scalar,
            DocumentPath targetPath
    ) {
        if (scalar.key() || isOpaqueValue(scalar.path().segments())) {
            return false;
        }
        List<String> segments = scalar.path().segments();
        if (isDiscriminatorMapping(segments)) {
            if (isSchemaComponentPath(targetPath) && scalar.value().equals(targetPath.segments().getLast())) {
                return true;
            }
            return pointsTo(scalar.value(), targetPath);
        }
        return !segments.isEmpty()
                && REF_FIELD.equals(segments.getLast())
                && isReferenceLocation(segments)
                && pointsTo(scalar.value(), targetPath);
    }

    static boolean isPotentialSchemaId(SourceScalar scalar) {
        return scalar.key()
                && ID_FIELD.equals(scalar.value())
                && !isOpaqueValue(scalar.path().segments());
    }

    private static boolean isOpaqueValue(List<String> segments) {
        for (int index = 0; index < segments.size(); index++) {
            if (isLiteralSchemaField(segments, index)
                    || isExtensionField(segments, index)
                    || isExamplePayload(segments, index)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReferenceLocation(List<String> segments) {
        return isSchemaComponentReference(segments)
                || isPathSchemaReference(segments)
                || isComponentReferenceObject(segments)
                || isPathReferenceObject(segments);
    }

    private static boolean isSchemaComponentReference(List<String> segments) {
        int referenceIndex = segments.size() - 1;
        for (int index = 0; index + 2 < referenceIndex; index++) {
            if (COMPONENTS_FIELD.equals(segments.get(index))
                    && SCHEMAS_FIELD.equals(segments.get(index + 1))) {
                return isSchemaObjectPath(segments, index + 3, referenceIndex);
            }
        }
        return false;
    }

    private static boolean isPathSchemaReference(List<String> segments) {
        int referenceIndex = segments.size() - 1;
        for (int index = 1; index < referenceIndex; index++) {
            if ("schema".equals(segments.get(index))
                    && isPathSchemaField(segments, index)
                    && isSchemaObjectPath(segments, index + 1, referenceIndex)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSchemaObjectPath(List<String> segments, int start, int end) {
        int index = start;
        while (index < end) {
            String field = segments.get(index);
            if (MAP_SCHEMA_FIELDS.contains(field)) {
                if (index + 1 >= end) {
                    return false;
                }
                index += 2;
            } else if (ARRAY_SCHEMA_FIELDS.contains(field)) {
                if (index + 1 >= end || !isArrayIndex(segments.get(index + 1))) {
                    return false;
                }
                index += 2;
            } else if (SINGLE_SCHEMA_FIELDS.contains(field)) {
                index++;
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean isPathSchemaField(List<String> segments, int schemaIndex) {
        List<String> parent = segments.subList(0, schemaIndex);
        if (isParameterOrHeader(parent)) {
            return true;
        }
        int contentIndex = schemaIndex - 2;
        if (contentIndex < 0 || !"content".equals(segments.get(contentIndex))) {
            return false;
        }
        return isContentContainer(segments.subList(0, contentIndex));
    }

    private static boolean isParameterOrHeader(List<String> parent) {
        if (parent.isEmpty()) {
            return false;
        }
        if ("paths".equals(parent.getFirst())) {
            return parent.size() == 4 && "parameters".equals(parent.get(2)) && isArrayIndex(parent.get(3))
                    || parent.size() == 5 && HTTP_METHODS.contains(parent.get(2))
                    && "parameters".equals(parent.get(3)) && isArrayIndex(parent.get(4))
                    || parent.size() == 7 && HTTP_METHODS.contains(parent.get(2))
                    && "responses".equals(parent.get(3)) && "headers".equals(parent.get(5));
        }
        if (parent.size() == 3 && COMPONENTS_FIELD.equals(parent.getFirst())) {
            return Set.of("headers", "parameters").contains(parent.get(1));
        }
        return parent.size() == 5
                && COMPONENTS_FIELD.equals(parent.getFirst())
                && "responses".equals(parent.get(1))
                && "headers".equals(parent.get(3));
    }

    private static boolean isContentContainer(List<String> parent) {
        if (parent.isEmpty()) {
            return false;
        }
        if ("paths".equals(parent.getFirst())) {
            return parent.size() == 4 && HTTP_METHODS.contains(parent.get(2)) && "requestBody".equals(parent.get(3))
                    || parent.size() == 5 && HTTP_METHODS.contains(parent.get(2))
                    && "responses".equals(parent.get(3))
                    || parent.size() == 4 && "parameters".equals(parent.get(2))
                    || parent.size() == 5 && HTTP_METHODS.contains(parent.get(2))
                    && "parameters".equals(parent.get(3))
                    || parent.size() == 7 && HTTP_METHODS.contains(parent.get(2))
                    && "responses".equals(parent.get(3)) && "headers".equals(parent.get(5));
        }
        return parent.size() == 3
                && COMPONENTS_FIELD.equals(parent.getFirst())
                && Set.of("headers", "parameters", "requestBodies", "responses").contains(parent.get(1))
                || parent.size() == 5
                && COMPONENTS_FIELD.equals(parent.getFirst())
                && "responses".equals(parent.get(1)) && "headers".equals(parent.get(3));
    }

    private static boolean isComponentReferenceObject(List<String> segments) {
        return segments.size() == 4
                && COMPONENTS_FIELD.equals(segments.getFirst())
                && COMPONENT_REFERENCE_FIELDS.contains(segments.get(1));
    }

    private static boolean isPathReferenceObject(List<String> segments) {
        if (segments.size() < 3 || !"paths".equals(segments.getFirst())) {
            return false;
        }
        List<String> parent = segments.subList(0, segments.size() - 1);
        if (parent.size() == 2) {
            return true;
        }
        return parent.size() == 4 && "parameters".equals(parent.get(2)) && isArrayIndex(parent.get(3))
                || parent.size() == 5 && HTTP_METHODS.contains(parent.get(2))
                && "parameters".equals(parent.get(3)) && isArrayIndex(parent.get(4))
                || parent.size() == 4 && HTTP_METHODS.contains(parent.get(2))
                && "requestBody".equals(parent.get(3))
                || parent.size() == 5 && HTTP_METHODS.contains(parent.get(2))
                && "responses".equals(parent.get(3))
                || parent.size() == 7 && HTTP_METHODS.contains(parent.get(2))
                && "responses".equals(parent.get(3)) && "headers".equals(parent.get(5));
    }

    private static boolean isLiteralSchemaField(List<String> segments, int index) {
        return isOasField(segments, index) && LITERAL_SCHEMA_FIELDS.contains(segments.get(index));
    }

    private static boolean isExtensionField(List<String> segments, int index) {
        return isOasField(segments, index) && segments.get(index).startsWith("x-");
    }

    private static boolean isExamplePayload(List<String> segments, int index) {
        if (!isOasField(segments, index)) {
            return false;
        }
        if ("examples".equals(segments.get(index))) {
            return hasSchemaExampleValue(segments, index) || hasExampleObjectValue(segments, index);
        }
        return isExampleObjectValue(segments, index);
    }

    private static boolean hasSchemaExampleValue(List<String> segments, int index) {
        return index + 1 < segments.size() && isArrayIndex(segments.get(index + 1));
    }

    private static boolean hasExampleObjectValue(List<String> segments, int index) {
        return index + 2 < segments.size()
                && isMapEntry(segments, index + 1)
                && "value".equals(segments.get(index + 2));
    }

    private static boolean isExampleObjectValue(List<String> segments, int index) {
        return "value".equals(segments.get(index))
                && index >= 2
                && "examples".equals(segments.get(index - 2))
                && isMapEntry(segments, index - 1);
    }

    private static boolean isArrayIndex(String segment) {
        return !segment.isEmpty() && segment.chars().allMatch(Character::isDigit);
    }

    private static boolean isMapEntry(List<String> segments, int index) {
        return index > 0 && NAMED_MAP_FIELDS.contains(segments.get(index - 1)) && !isMapEntry(segments, index - 1);
    }

    private static boolean isOasField(List<String> segments, int index) {
        return !isMapEntry(segments, index);
    }

    private String relocateReference(String value) {
        try {
            URI uri = new URI(value);
            if (!isLocalFragment(uri)) {
                return value;
            }
            DocumentPath target = DocumentPath.fromFragment("#" + uri.getRawFragment());
            if (!target.startsWith(oldPath)) {
                return value;
            }
            List<String> relocated = new ArrayList<>(newPath.segments());
            relocated.addAll(target.segments().subList(oldPath.segments().size(), target.segments().size()));
            return new DocumentPath(relocated).toFragment();
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return value;
        }
    }

    private boolean pointsTo(
            String value,
            DocumentPath targetPath
    ) {
        try {
            URI uri = new URI(value);
            if (!isLocalFragment(uri)) {
                return false;
            }
            DocumentPath target = DocumentPath.fromFragment("#" + uri.getRawFragment());
            return target.startsWith(targetPath);
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isRequiredValue(SourceScalar scalar) {
        return requiredPath != null
                && scalar.path().parent().filter(requiredPath::equals).isPresent();
    }

    private boolean isSchemaComponentPath(DocumentPath path) {
        List<String> segments = path.segments();
        return segments.size() == 3
                && COMPONENTS_FIELD.equals(segments.get(0))
                && SCHEMAS_FIELD.equals(segments.get(1));
    }

    private boolean isDiscriminatorMapping(List<String> segments) {
        return segments.size() >= 3 && MAPPING_FIELD.equals(segments.get(segments.size() - 2))
                && DISCRIMINATOR_FIELD.equals(segments.get(segments.size() - 3));
    }

    private static boolean isLocalFragment(URI uri) {
        return uri.getScheme() == null
                && uri.getRawAuthority() == null
                && uri.getRawQuery() == null
                && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                && uri.getRawFragment() != null;
    }
}
