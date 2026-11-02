package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import java.util.function.BiConsumer;

final class SchemaChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, String path, BiConsumer<Object, String> visitor) {
        if (node instanceof SchemaComponent component) {
            visitor.accept(component.getSchema(), path + ".schema");
        } else if (node instanceof SchemaProperty property) {
            visitor.accept(property.getSchema(), path + ".schema");
        } else if (node instanceof SchemaObject schema) {
            visitor.accept(schema.getProperties(), path + ".properties");
            visitor.accept(schema.getAdditionalProperties(), path + ".additionalProperties");
            visitor.accept(schema.getItems(), path + ".items");
            visitor.accept(schema.getAllOf(), path + ".allOf");
            visitor.accept(schema.getOneOf(), path + ".oneOf");
            visitor.accept(schema.getAnyOf(), path + ".anyOf");
            visitor.accept(schema.getNot(), path + ".not");
            visitor.accept(schema.getDiscriminator(), path + ".discriminator");
        } else {
            return node instanceof BooleanSchema || node instanceof Discriminator;
        }
        return true;
    }
}
