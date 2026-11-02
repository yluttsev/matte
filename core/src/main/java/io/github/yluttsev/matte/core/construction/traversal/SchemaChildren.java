package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.Discriminator;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import java.util.function.BiConsumer;

final class SchemaChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, BiConsumer<Object, String> visitor) {
        if (node instanceof SchemaComponent component) {
            visitor.accept(component.getSchema(), "schema");
        } else if (node instanceof SchemaProperty property) {
            visitor.accept(property.getSchema(), "schema");
        } else if (node instanceof SchemaObject schema) {
            visitor.accept(schema.getProperties(), "properties");
            visitor.accept(schema.getAdditionalProperties(), "additionalProperties");
            visitor.accept(schema.getItems(), "items");
            visitor.accept(schema.getAllOf(), "allOf");
            visitor.accept(schema.getOneOf(), "oneOf");
            visitor.accept(schema.getAnyOf(), "anyOf");
            visitor.accept(schema.getNot(), "not");
            visitor.accept(schema.getDiscriminator(), "discriminator");
        } else {
            return node instanceof BooleanSchema || node instanceof Discriminator;
        }
        return true;
    }
}
