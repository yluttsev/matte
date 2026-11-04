package io.github.yluttsev.matte.openapi.patch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

final class SchemaReferenceRelocator {

    private static final String REF_FIELD = "$ref";
    private static final String DISCRIMINATOR_FIELD = "discriminator";
    private static final String MAPPING_FIELD = "mapping";

    private final String oldName;
    private final String newName;
    private final DocumentPath oldPath;
    private final DocumentPath newPath;

    SchemaReferenceRelocator(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
        oldPath = DocumentPath.schemaComponent(oldName);
        newPath = DocumentPath.schemaComponent(newName);
    }

    DocumentPath oldPath() {
        return oldPath;
    }

    String updatedValue(SourceScalar scalar) {
        List<String> segments = scalar.path().segments();
        if (isOpaqueValue(segments)) {
            return scalar.value();
        }
        if (!segments.isEmpty() && REF_FIELD.equals(segments.getLast())) {
            return relocateReference(scalar.value());
        }
        if (segments.size() >= 3 && MAPPING_FIELD.equals(segments.get(segments.size() - 2))
                && DISCRIMINATOR_FIELD.equals(segments.get(segments.size() - 3))) {
            return scalar.value().equals(oldName) ? newName : relocateReference(scalar.value());
        }
        return scalar.value();
    }

    private static boolean isOpaqueValue(List<String> segments) {
        boolean inExamples = false;
        for (String segment : segments) {
            if (segment.startsWith("x-") || "example".equals(segment)) {
                return true;
            }
            if ("examples".equals(segment)) {
                inExamples = true;
            } else if (inExamples && "value".equals(segment)) {
                return true;
            }
        }
        return false;
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

    private static boolean isLocalFragment(URI uri) {
        return uri.getScheme() == null
                && uri.getRawAuthority() == null
                && uri.getRawQuery() == null
                && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                && uri.getRawFragment() != null;
    }
}
