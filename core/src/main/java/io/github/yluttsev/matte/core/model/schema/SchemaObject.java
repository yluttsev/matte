package io.github.yluttsev.matte.core.model.schema;

import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.value.DocumentValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SchemaObject implements Schema {

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
}
