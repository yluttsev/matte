# Reference locations in schema edits

[Русский](../../ru/openapi/references.md)

Schema component and property rename/delete share the same source reference classification. It follows object roles recursively rather than matching paths of a fixed depth:

- Schema Objects in `components.schemas`, parameter/header `schema`, and Media Type `schema`; nested schema maps (`properties`, `patternProperties`, `dependentSchemas`, `$defs`, `definitions`), single-schema keywords (including `items`, `additionalProperties`, `contains`, `if`/`then`/`else`, `unevaluatedItems`, `unevaluatedProperties`, `contentSchema`) and schema arrays (`allOf`, `anyOf`, `oneOf`, `prefixItems`). Discriminator mapping values are also relocated.
- Path Items in `paths`, OAS 3.1 `webhooks` and `components.pathItems`, and callback expressions under operation callbacks or `components.callbacks`. Callback nesting can repeat at any depth. Path/operation parameters, operation request bodies and responses use the same rules in every Path Item.
- Media Types in request-body/response content and parameter/header content. Encoding headers recurse through the same Header and Media Type roles, including nested `encoding.*.headers`.
- Reference Objects in reusable parameters, headers, request bodies, responses, callbacks, examples, links, security schemes and path items; inline parameter, header, request-body, response, callback, example and link positions; and the Path Item `$ref` field. This classification locates reference fields; it does not validate that their target has the required OpenAPI object type.

Every source `$ref` is classified as recognized, opaque (literal or extension payload), or unrecognized. Schema literal keywords, parameter/media examples, Example Object `value`, Link Object parameter/request-body values and `x-*` payloads are opaque. Named map entries such as a property or header named `x-name` or `example`, and discriminator mapping labels including `$ref`, remain names rather than literal/extension fields. Extensions of the `paths` object are opaque too.

A recognized local pointer to a renamed component/property or its descendants is relocated. An unrecognized `$ref` targeting that subtree refuses rename with `Refused(UNRECOGNIZED_REFERENCE)` listing all unrecognized locations. Delete returns `Rejected` carrying `EditRejected(REFERENCED_TARGET)` listing all incoming recognized and unrecognized locations outside the removed subtree, including locations absent from the semantic model. Unrelated, external and opaque values remain unchanged. Refusal leaves the input model and original bytes unchanged; no partial edit is published. These rules do not add external-reference resolution or remove the existing OAS 3.1 `$id` scope restriction.

The versioned YAML/JSON catalogues `reference-locations-3.0.*` and `reference-locations-3.1.*` in the module test resources, with their location manifests and expected renamed documents, provide a reusable inventory of these reference roles.

See the [editing result contract](editing.md) for result variants and atomicity.
