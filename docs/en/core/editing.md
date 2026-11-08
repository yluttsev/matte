# Document Editing (`core.editing`)

[Русский](../../ru/core/editing.md)

This package handles atomic execution of semantic commands against a document, protects the model from uncontrolled mutation, and produces an ordered record of changes (`ChangeSet`).

`DocumentEditor` accepts a list of `EditCommandHandler` instances through its constructor. The no-argument constructor supplies the current schema handlers for existing callers. An application composition root can construct and inject the handlers without changing the dispatcher; the dispatcher takes an immutable copy of the supplied list.

See the [OpenAPI editing contract](../openapi/editing.md) for source-aware results (`Applied`, `Rejected`, `Refused`), refusal codes and atomicity guarantees.

## Editing Session and Mutation Control

`DocumentEditor.apply(document, command)` never mutates the input document instance. Every operation executes atomically in the following sequence:
1. An independent deep working copy of the document is created and indexed with `DocumentIndex`.
2. A package-private `MutationAccess` token is opened for the working copy, registering all node instances in that graph.
3. Every mutating method on the domain model checks the `MutationAccess` token to verify that the session is still active and that the target node belongs to the current working copy.
4. The dispatcher requires exactly one registered handler to support the command's exact type, invokes that handler once, and collects its `Change` records. Missing handlers and overlapping registrations are configuration errors.
5. The modified working copy is validated with `DocumentStructureValidator`. If the command is rejected or the resulting structure is invalid, the working copy is discarded.
6. In a `finally` block, the `MutationAccess` token is closed; any subsequent attempt to invoke mutating methods on the returned document throws `IllegalStateException`.

## Edit Results (`EditResult`)

Applying a command returns one of two implementations of the sealed interface `EditResult`:
- `EditSucceeded` — contains the updated `ApiDocument` instance and an immutable `ChangeSet` listing the changes in execution order;
- `EditRejected` — contains a non-empty list of `EditIssue` items (each with an `EditCode` and a diagnostic message).

Rejection codes (`EditCode`):
- `TARGET_NOT_FOUND` — no element with the specified `ElementId` exists in the document;
- `TARGET_TYPE_MISMATCH` — the target element exists, but its type is incompatible with the command (for example, passing the ID of a `BooleanSchema` or `SchemaComponent` instead of a `SchemaObject` when adding a property);
- `INVALID_NAME` — a new component name does not match `^[a-zA-Z0-9.\-_]+$`, or a property name (including nested properties within an added subtree) is empty or blank;
- `NAME_CONFLICT` — an element with the same name already exists in the target collection;
- `ID_CONFLICT` — the added subtree contains an `ElementId` that already exists in the document or is duplicated within the added subtree itself;
- `REFERENCED_TARGET` — deleting a schema component or property would leave an incoming reference unresolved; the diagnostic lists each reference location;
- `INVALID_STRUCTURE` — the resulting document fails `DocumentStructureValidator` checks after applying the command.

## Commands (`EditCommand`) and Changes (`Change`)

Supported schema editing commands:
- `AddSchemaComponent` — appends a new schema to `components/schemas` and emits `SchemaComponentAdded`;
- `AddSchemaProperty` — locates the target `SchemaObject` by its `ElementId` (whether a top-level component schema or a nested inline schema), appends a new property, and emits `SchemaPropertyAdded`;
- `RenameSchemaProperty` — locates a property by its `ElementId` within the specified `SchemaObject`, preserves its identity, renames it, replaces the matching `required` name in place (keeping its position, or dropping the old name when the new one is already listed), and rewrites local pointers to the property and its descendants;
- `DeleteSchemaProperty` — removes a property by its `ElementId` when no reference from outside its subtree targets it, and emits `SchemaPropertyDeleted`;
- `DeleteSchemaComponent` — removes a schema component when no reference from outside its subtree targets it, and emits `SchemaComponentDeleted`;
- `UpdateSchemaObject` — replaces the fields of an existing `SchemaObject` while preserving its `ElementId`; `SchemaObjectUpdated` records the schema path and before/after snapshots;
- `RenameSchemaComponent` — locates a schema component by its `ElementId`, renames it, and automatically rewrites all applicable local references across the document that point to that component or its descendants.

`AddSchemaComponent` and `RenameSchemaComponent` validate the destination name with a shared component-name validator. Names must contain one or more ASCII letters, digits, dots, hyphens or underscores, following the same OpenAPI component key pattern in OAS 3.0 and 3.1. Validation runs before the rename no-op check: an unchanged invalid name is rejected as well. This rule applies to editing commands, not model construction or import: existing invalid component names remain readable and can be renamed to valid names. Property names do not follow the component key pattern; their existing non-blank requirement remains unchanged. A rejected command leaves the original document untouched.

Use `SchemaObject.builder(existingSchema)` to prepare an update without losing unchanged fields or nested element identifiers. Pass `null` to a nullable builder field to remove that field, and use an empty collection to clear a list or set field. A literal null in `default`, `example`, `examples`, `enum`, or `const` is represented by `NullValue` and is different from removing the field.

When a schema component is renamed, the `ChangeSet` records:
- `SchemaComponentRenamed` — the component's `ElementId`, old name, and new name;
- `ReferenceChanged` — one entry per updated reference (recording `sourceBefore` and `sourceAfter` paths, old and new string values, and `ReferenceKind`). If the new name equals the current name, an isolated copy of the document and an empty `ChangeSet` are returned.

When a property is renamed, the `ChangeSet` records `SchemaPropertyRenamed` followed by one `ReferenceChanged` per relocated local reference. Pointers to nested schemas inside the property are relocated with the property, and the owning schema's `required` set is updated when it contains the old property name. Deleting a property leaves the schema's `required` set untouched: `required` is an independent declaration and may name properties that are inherited or not declared in the local `properties` list. References whose source is inside the deleted subtree are removed with it and do not block deletion; references from outside the subtree do.

Schema source deletion retains an empty `properties` or `schemas` section as `{}` instead of removing the section. YAML deletion rejects ambiguous leading comments, trailing inline comments, flow-mapping separator comments, or standalone comments between the deleted node and its sibling. Comments inside the deleted node are removed with it. These source-level failures leave the caller's original source and model unchanged.

## Reference Rewriting (`ReferenceOwner`)

All model nodes that can hold direct references (`SchemaObject`, `Discriminator`, `NamedComponent`, `Operation`, `PathItem`, `ApiResponse`, `Parameter`, `Header`, `MediaType`, `Encoding`) implement `ReferenceOwner`.

When renaming a schema component:
- local fragment pointers targeting `#/components/schemas/<OldName>` or nested paths `#/components/schemas/<OldName>/...` are rewritten to `<NewName>` with proper JSON Pointer and URI escaping;
- short schema names in `Discriminator.mapping` matching the old name are replaced with the new name; an existing schema name takes precedence over interpreting the same text as a URI;
- rewritten `ObjectReference` instances preserve their `ElementId`, `summary`, and `description`;
- external references, unsupported anchors, malformed pointers, indirect references (reaching the schema through an intermediate alias), and literal payloads inside `DocumentValue` (`example`, `default`) remain untouched.
