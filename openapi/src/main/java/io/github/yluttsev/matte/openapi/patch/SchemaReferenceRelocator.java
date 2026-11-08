package io.github.yluttsev.matte.openapi.patch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

final class SchemaReferenceRelocator {

    private static final String REF_FIELD = "$ref";
    private static final String ID_FIELD = "$id";
    private static final String COMPONENTS_FIELD = "components";
    private static final String SCHEMAS_FIELD = "schemas";
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
        return OpenApiReferenceLocation.classify(segments) == OpenApiReferenceLocation.Kind.OPAQUE;
    }

    private static boolean isReferenceLocation(List<String> segments) {
        return OpenApiReferenceLocation.classify(segments) == OpenApiReferenceLocation.Kind.REFERENCE;
    }

    List<DocumentPath> unrecognizedReferences(List<SourceScalar> scalars) {
        return scalars.stream()
                .filter(scalar -> !scalar.key())
                .filter(scalar -> !scalar.path().segments().isEmpty())
                .filter(scalar -> REF_FIELD.equals(scalar.path().segments().getLast()))
                .filter(scalar -> OpenApiReferenceLocation.classify(scalar.path().segments())
                        == OpenApiReferenceLocation.Kind.UNKNOWN)
                .filter(scalar -> pointsTo(scalar.value(), oldPath))
                .map(SourceScalar::path)
                .distinct()
                .toList();
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
        return OpenApiReferenceLocation.classify(segments) == OpenApiReferenceLocation.Kind.MAPPING_VALUE;
    }

    private static boolean isLocalFragment(URI uri) {
        return uri.getScheme() == null
                && uri.getRawAuthority() == null
                && uri.getRawQuery() == null
                && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                && uri.getRawFragment() != null;
    }
}
