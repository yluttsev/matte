# Domain Model (`core.model`)

[Русский](../../ru/core/model.md)

This package defines the semantic structure of an API document and all of its building blocks.

## Document Structure

The root object is `ApiDocument`. It brings together:
- general API metadata (`ApiInfo`, `Contact`, `License`, `ExternalDocumentation`);
- servers and their variables (`Server`, `ServerVariable`);
- paths and HTTP operations (`PathItem`, `Operation`, `HttpMethod`);
- reusable components (`Components`);
- tags (`Tag`) and security definitions (`SecurityRequirement`, `SecurityScheme`, `OAuthFlows`, `OAuthFlow`);
- parameters, headers, request bodies, media types, and responses (`Parameter`, `Header`, `RequestBody`, `MediaType`, `Encoding`, `Example`, `ApiResponse`).

## Identity and Named Elements

Elements that require persistent identity across edits (the document itself, paths, operations, parameters, schemas, schema properties, components, etc.) implement `IdentifiedElement` and carry an `ElementId`.

Where the specification represents elements as "name → definition" pairs that can be renamed, the model uses ordered lists of wrapper objects with their own identifiers instead of a `Map`:
- document paths are stored as a list of `PathItem` instances (each with its own identifier and path string);
- component schemas are stored as a list of `SchemaComponent` instances;
- schema properties are stored as a list of `SchemaProperty` instances;
- all other component sections (`parameters`, `responses`, `requestBodies`, `headers`, `securitySchemes`, `examples`) are stored as lists of `NamedComponent` instances.

This preserves element ordering and allows renaming elements without losing their internal identifier.

## Schema Model (`core.model.schema`)

A schema is represented by the sealed interface `Schema` with two implementations:
- `BooleanSchema` — a boolean schema (`true` or `false`), used for example in `additionalProperties` or `items`;
- `SchemaObject` — a unified compositional schema model.

Schemas are not split into a rigid hierarchy of primitives, arrays, and objects. A single `SchemaObject` instance can combine a set of types (`Set<JsonType>`), object properties (`properties`, `required`, `additionalProperties`), array item definitions (`items`), numeric and string constraints, composition keywords (`allOf`, `oneOf`, `anyOf`, `not`), a `Discriminator`, and a schema reference (`SchemaReference`).

Required property names are stored separately from the property list (`Set<String> required`), allowing a schema to mark properties as required even if they have not yet been added to `properties` or are inherited through composition.

The six object, array and string size constraints (`minProperties`, `maxProperties`, `minItems`, `maxItems`, `minLength`, `maxLength`) use `BigInteger`; the model has no 32-bit integer limit for these values. Schema literal numbers use `BigDecimal` through `NumberValue`.

`JsonType.NULL` represents the JSON null type. OpenAPI 3.0 `nullable: true` is represented together with the declared type, while OpenAPI 3.1 type unions are represented by multiple `JsonType` values. `SchemaObject.builder(existingSchema)` starts from a deep copy, so changing one field can preserve the remaining values and nested element identities.

## Inline Objects and References (`core.model.reference`)

Unlike schemas, where `$ref` can coexist with other keywords inside the same object, other specification elements (parameters, request bodies, responses, headers, examples, security schemes) are either an inline definition or a reference.

They are modeled using the sealed interface `ReferenceOr<T>`:
- `InlineObject<T>` — holds the inline domain object itself;
- `ObjectReference<T>` — holds the reference identifier, the `$ref` string, and optional `summary` and `description` overrides.

## Data Values (`core.model.value`)

Arbitrary literal values inside a document (`default`, `example`, `examples`, `enum`, `const`) are represented by the sealed interface `DocumentValue` and its implementations: `NullValue`, `BooleanValue`, `NumberValue`, `StringValue`, `ArrayValue`, `ObjectValue`.

This representation makes it possible to:
- clearly distinguish an omitted field (Java `null`) from an explicit null literal (`new NullValue()`);
- keep arbitrary user example payloads separate from actual document nodes and `$ref` references.

## Operation Security Override

In OpenAPI, omitting `security` on an operation inherits the document-level security requirements, whereas an explicit empty list `security: []` disables security for that operation.

To distinguish between these two states, `Operation` provides the `hasSecurityOverride()` flag:
- calling `builder.security(...)` (including with an empty list) sets the flag to `true`;
- by default (or after calling `builder.inheritSecurity()`), the flag is `false` and the operation inherits document-level security.
