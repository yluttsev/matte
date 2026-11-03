package io.github.yluttsev.matte.core.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import io.github.yluttsev.matte.core.construction.CopyableModel;
import io.github.yluttsev.matte.core.model.reference.ObjectReference;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.navigation.DocumentEntry;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;

public final class ReferenceIndexBuilder {

    private static final String REF_SEGMENT = "$ref";
    private static final String MAPPING_SEGMENT = "mapping";

    private final ReferenceResolver resolver = new ReferenceResolver();

    public ReferenceIndex build(DocumentIndex document) {
        Objects.requireNonNull(document, "document");
        List<ReferenceUsage<?>> usages = new ArrayList<>();
        for (DocumentEntry entry : document.entries()) {
            if (entry.value() instanceof SchemaObject schema && schema.getReference() != null) {
                usages.add(usage(document, entry.path().child(REF_SEGMENT), schema.getReference().value(),
                        ReferenceKind.SCHEMA, Schema.class));
            } else if (entry.value() instanceof ObjectReference<?> reference) {
                Class<? extends CopyableModel<?>> type = ReferenceTypes.at(entry.path())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown reference type at " + entry.path()));
                usages.add(usage(document, entry.path().child(REF_SEGMENT), reference.value(),
                        ReferenceKind.OBJECT, type));
            } else if (entry.value() instanceof Discriminator discriminator) {
                addDiscriminatorUsages(document, entry.path(), discriminator, usages);
            }
        }
        return new ReferenceIndex(document, usages);
    }

    private void addDiscriminatorUsages(
            DocumentIndex document,
            DocumentPath path,
            Discriminator discriminator,
            List<ReferenceUsage<?>> usages
    ) {
        discriminator.getMapping().forEach((name, reference) -> {
            String original = reference.value();
            String target = discriminatorTarget(document, original);
            usages.add(new ReferenceUsage<>(path.child(MAPPING_SEGMENT).child(name), original,
                    ReferenceKind.DISCRIMINATOR_MAPPING, Schema.class,
                    resolver.resolve(document, target, Schema.class)));
        });
    }

    private String discriminatorTarget(DocumentIndex document, String mappingValue) {
        DocumentPath namedSchema = DocumentPath.schemaComponent(mappingValue);
        return document.findByPath(namedSchema).isPresent() ? namedSchema.toFragment() : mappingValue;
    }

    private <T extends CopyableModel<?>> ReferenceUsage<T> usage(
            DocumentIndex document,
            DocumentPath source,
            String reference,
            ReferenceKind kind,
            Class<T> type
    ) {
        return new ReferenceUsage<>(source, reference, kind, type, resolver.resolve(document, reference, type));
    }
}
