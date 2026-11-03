package io.github.yluttsev.matte.core.model.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import io.github.yluttsev.matte.core.construction.ModelCopies;
import io.github.yluttsev.matte.core.editing.MutationAccess;
import io.github.yluttsev.matte.core.editing.ReferenceOwner;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

public final class SchemaObject implements Schema, ReferenceOwner {

    private final ElementId id;
    private final Set<JsonType> types = new LinkedHashSet<>();
    private String format;
    private String title;
    private String description;
    private SchemaReference reference;
    private DocumentValue defaultValue;
    private DocumentValue example;
    private final List<DocumentValue> examples = new ArrayList<>();
    private final List<DocumentValue> enumValues = new ArrayList<>();
    private DocumentValue constValue;
    private Boolean deprecated;
    private Boolean readOnly;
    private Boolean writeOnly;

    private final List<SchemaProperty> properties = new ArrayList<>();
    private final Set<String> required = new LinkedHashSet<>();
    private Schema additionalProperties;
    private BigInteger minProperties;
    private BigInteger maxProperties;

    private Schema items;
    private BigInteger minItems;
    private BigInteger maxItems;
    private Boolean uniqueItems;

    private BigInteger minLength;
    private BigInteger maxLength;
    private String pattern;

    private BigDecimal minimum;
    private BigDecimal maximum;
    private BigDecimal exclusiveMinimum;
    private BigDecimal exclusiveMaximum;
    private BigDecimal multipleOf;

    private final List<Schema> allOf = new ArrayList<>();
    private final List<Schema> oneOf = new ArrayList<>();
    private final List<Schema> anyOf = new ArrayList<>();
    private Schema not;
    private Discriminator discriminator;

    public SchemaObject(ElementId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    private SchemaObject(SchemaObject source) {
        this(source.id);
        types.addAll(source.types);
        format = source.format;
        title = source.title;
        description = source.description;
        reference = source.reference;
        defaultValue = source.defaultValue;
        example = source.example;
        examples.addAll(source.examples);
        enumValues.addAll(source.enumValues);
        constValue = source.constValue;
        deprecated = source.deprecated;
        readOnly = source.readOnly;
        writeOnly = source.writeOnly;
        properties.addAll(ModelCopies.list(source.properties));
        required.addAll(source.required);
        additionalProperties = ModelCopies.copy(source.additionalProperties);
        minProperties = source.minProperties;
        maxProperties = source.maxProperties;
        items = ModelCopies.copy(source.items);
        minItems = source.minItems;
        maxItems = source.maxItems;
        uniqueItems = source.uniqueItems;
        minLength = source.minLength;
        maxLength = source.maxLength;
        pattern = source.pattern;
        minimum = source.minimum;
        maximum = source.maximum;
        exclusiveMinimum = source.exclusiveMinimum;
        exclusiveMaximum = source.exclusiveMaximum;
        multipleOf = source.multipleOf;
        allOf.addAll(ModelCopies.list(source.allOf));
        oneOf.addAll(ModelCopies.list(source.oneOf));
        anyOf.addAll(ModelCopies.list(source.anyOf));
        not = ModelCopies.copy(source.not);
        discriminator = ModelCopies.copy(source.discriminator);
    }

    public static Builder builder(ElementId id) {
        return new Builder(id);
    }

    @Override
    public SchemaObject copy() {
        return new SchemaObject(this);
    }

    @Override
    public ElementId getId() {
        return id;
    }

    public Set<JsonType> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    public String getFormat() {
        return format;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public SchemaReference getReference() {
        return reference;
    }

    public DocumentValue getDefaultValue() {
        return defaultValue;
    }

    public DocumentValue getExample() {
        return example;
    }

    public List<DocumentValue> getExamples() {
        return Collections.unmodifiableList(examples);
    }

    public List<DocumentValue> getEnumValues() {
        return Collections.unmodifiableList(enumValues);
    }

    public DocumentValue getConstValue() {
        return constValue;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Boolean getWriteOnly() {
        return writeOnly;
    }

    public List<SchemaProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    public Set<String> getRequired() {
        return Collections.unmodifiableSet(required);
    }

    public Schema getAdditionalProperties() {
        return additionalProperties;
    }

    public BigInteger getMinProperties() {
        return minProperties;
    }

    public BigInteger getMaxProperties() {
        return maxProperties;
    }

    public Schema getItems() {
        return items;
    }

    public BigInteger getMinItems() {
        return minItems;
    }

    public BigInteger getMaxItems() {
        return maxItems;
    }

    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    public BigInteger getMinLength() {
        return minLength;
    }

    public BigInteger getMaxLength() {
        return maxLength;
    }

    public String getPattern() {
        return pattern;
    }

    public BigDecimal getMinimum() {
        return minimum;
    }

    public BigDecimal getMaximum() {
        return maximum;
    }

    public BigDecimal getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public BigDecimal getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public BigDecimal getMultipleOf() {
        return multipleOf;
    }

    public List<Schema> getAllOf() {
        return Collections.unmodifiableList(allOf);
    }

    public List<Schema> getOneOf() {
        return Collections.unmodifiableList(oneOf);
    }

    public List<Schema> getAnyOf() {
        return Collections.unmodifiableList(anyOf);
    }

    public Schema getNot() {
        return not;
    }

    public Discriminator getDiscriminator() {
        return discriminator;
    }

    public void addProperty(MutationAccess access, SchemaProperty property) {
        access.require(this);
        properties.add(Objects.requireNonNull(property, "property").copy());
    }

    @Override
    public void rewriteReferences(MutationAccess access, UnaryOperator<String> transform) {
        access.require(this);
        if (reference != null) {
            reference = new SchemaReference(transform.apply(reference.value()));
        }
    }

    public static final class Builder {

        private final SchemaObject draft;

        private Builder(ElementId id) {
            draft = new SchemaObject(id);
        }

        public Builder types(Set<JsonType> types) {
            List<JsonType> values = List.copyOf(types);
            draft.types.clear();
            draft.types.addAll(values);
            return this;
        }

        public Builder format(String format) {
            draft.format = format;
            return this;
        }

        public Builder title(String title) {
            draft.title = title;
            return this;
        }

        public Builder description(String description) {
            draft.description = description;
            return this;
        }

        public Builder reference(SchemaReference reference) {
            draft.reference = reference;
            return this;
        }

        public Builder defaultValue(DocumentValue defaultValue) {
            draft.defaultValue = defaultValue;
            return this;
        }

        public Builder example(DocumentValue example) {
            draft.example = example;
            return this;
        }

        public Builder examples(List<DocumentValue> examples) {
            List<DocumentValue> values = List.copyOf(examples);
            draft.examples.clear();
            draft.examples.addAll(values);
            return this;
        }

        public Builder enumValues(List<DocumentValue> enumValues) {
            List<DocumentValue> values = List.copyOf(enumValues);
            draft.enumValues.clear();
            draft.enumValues.addAll(values);
            return this;
        }

        public Builder constValue(DocumentValue constValue) {
            draft.constValue = constValue;
            return this;
        }

        public Builder deprecated(Boolean deprecated) {
            draft.deprecated = deprecated;
            return this;
        }

        public Builder readOnly(Boolean readOnly) {
            draft.readOnly = readOnly;
            return this;
        }

        public Builder writeOnly(Boolean writeOnly) {
            draft.writeOnly = writeOnly;
            return this;
        }

        public Builder properties(List<SchemaProperty> properties) {
            List<SchemaProperty> values = List.copyOf(properties);
            draft.properties.clear();
            draft.properties.addAll(values);
            return this;
        }

        public Builder required(Set<String> required) {
            List<String> values = List.copyOf(required);
            draft.required.clear();
            draft.required.addAll(values);
            return this;
        }

        public Builder additionalProperties(Schema additionalProperties) {
            draft.additionalProperties = additionalProperties;
            return this;
        }

        public Builder minProperties(BigInteger minProperties) {
            draft.minProperties = minProperties;
            return this;
        }

        public Builder maxProperties(BigInteger maxProperties) {
            draft.maxProperties = maxProperties;
            return this;
        }

        public Builder items(Schema items) {
            draft.items = items;
            return this;
        }

        public Builder minItems(BigInteger minItems) {
            draft.minItems = minItems;
            return this;
        }

        public Builder maxItems(BigInteger maxItems) {
            draft.maxItems = maxItems;
            return this;
        }

        public Builder uniqueItems(Boolean uniqueItems) {
            draft.uniqueItems = uniqueItems;
            return this;
        }

        public Builder minLength(BigInteger minLength) {
            draft.minLength = minLength;
            return this;
        }

        public Builder maxLength(BigInteger maxLength) {
            draft.maxLength = maxLength;
            return this;
        }

        public Builder pattern(String pattern) {
            draft.pattern = pattern;
            return this;
        }

        public Builder minimum(BigDecimal minimum) {
            draft.minimum = minimum;
            return this;
        }

        public Builder maximum(BigDecimal maximum) {
            draft.maximum = maximum;
            return this;
        }

        public Builder exclusiveMinimum(BigDecimal exclusiveMinimum) {
            draft.exclusiveMinimum = exclusiveMinimum;
            return this;
        }

        public Builder exclusiveMaximum(BigDecimal exclusiveMaximum) {
            draft.exclusiveMaximum = exclusiveMaximum;
            return this;
        }

        public Builder multipleOf(BigDecimal multipleOf) {
            draft.multipleOf = multipleOf;
            return this;
        }

        public Builder allOf(List<Schema> allOf) {
            List<Schema> values = List.copyOf(allOf);
            draft.allOf.clear();
            draft.allOf.addAll(values);
            return this;
        }

        public Builder oneOf(List<Schema> oneOf) {
            List<Schema> values = List.copyOf(oneOf);
            draft.oneOf.clear();
            draft.oneOf.addAll(values);
            return this;
        }

        public Builder anyOf(List<Schema> anyOf) {
            List<Schema> values = List.copyOf(anyOf);
            draft.anyOf.clear();
            draft.anyOf.addAll(values);
            return this;
        }

        public Builder not(Schema not) {
            draft.not = not;
            return this;
        }

        public Builder discriminator(Discriminator discriminator) {
            draft.discriminator = discriminator;
            return this;
        }

        public SchemaObject build() {
            return draft.copy();
        }
    }
}
