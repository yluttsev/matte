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

    private final ReferenceResolver resolver = new ReferenceResolver();

    public ReferenceIndex build(DocumentIndex document) {
        Objects.requireNonNull(document, "document");
        List<ReferenceUsage<?>> usages = new ArrayList<>();
        for (DocumentEntry entry : document.entries()) {
            if (entry.value() instanceof SchemaObject schema && schema.getReference() != null) {
                usages.add(usage(document, entry.path().child("$ref"), schema.getReference().value(),
                        ReferenceKind.SCHEMA, Schema.class));
            } else if (entry.value() instanceof ObjectReference<?> reference) {
                Class<? extends CopyableModel<?>> type = ReferenceTypes.at(entry.path())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown reference type at " + entry.path()));
                usages.add(usage(document, entry.path().child("$ref"), reference.value(), ReferenceKind.OBJECT, type));
            } else if (entry.value() instanceof Discriminator discriminator) {
                discriminator.getMapping().forEach((name, reference) -> {
                    String original = reference.value();
                    DocumentPath namedSchema = DocumentPath.root().child("components").child("schemas").child(original);
                    String target = document.findByPath(namedSchema).isPresent() ? namedSchema.toFragment() : original;
                    usages.add(new ReferenceUsage<>(entry.path().child("mapping").child(name), original,
                            ReferenceKind.DISCRIMINATOR_MAPPING, Schema.class,
                            resolver.resolve(document, target, Schema.class)));
                });
            }
        }
        return new ReferenceIndex(document, usages);
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
