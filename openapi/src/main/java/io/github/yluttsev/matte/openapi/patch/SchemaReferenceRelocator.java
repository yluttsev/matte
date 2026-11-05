package io.github.yluttsev.matte.openapi.patch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

final class SchemaReferenceRelocator {

    private static final String REF_FIELD = "$ref";
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
        if (!segments.isEmpty() && REF_FIELD.equals(segments.getLast())) {
            return relocateReference(scalar.value());
        }
        if (isDiscriminatorMapping(segments)) {
            if (oldMappingName != null && scalar.value().equals(oldMappingName)) {
                return newMappingName;
            }
            return relocateReference(scalar.value());
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
        if (!segments.isEmpty() && REF_FIELD.equals(segments.getLast())) {
            return pointsTo(scalar.value(), targetPath);
        }
        if (!isDiscriminatorMapping(segments)) {
            return false;
        }
        if (isSchemaComponentPath(targetPath) && scalar.value().equals(targetPath.segments().getLast())) {
            return true;
        }
        return pointsTo(scalar.value(), targetPath);
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
