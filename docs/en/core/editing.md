# Document Editing (`core.editing`)

[Русский](../../ru/core/editing.md)

This package handles atomic execution of semantic commands against a document, protects the model from uncontrolled mutation, and produces an ordered record of changes (`ChangeSet`).

## Editing Session and Mutation Control

`DocumentEditor.apply(document, command)` never mutates the input document instance. Every operation executes atomically in the following sequence:
1. An independent deep working copy of the document is created and indexed with `DocumentIndex`.
2. A package-private `MutationAccess` token is opened for the working copy, registering all node instances in that graph.
3. Every mutating method on the domain model checks the `MutationAccess` token to verify that the session is still active and that the target node belongs to the current working copy.
4. The command handler applies the command and collects `Change` records.
5. The modified working copy is validated with `DocumentStructureValidator`. If the command is rejected or the resulting structure is invalid, the working copy is discarded.
6. In a `finally` block, the `MutationAccess` token is closed; any subsequent attempt to invoke mutating methods on the returned document throws `IllegalStateException`.

## Edit Results (`EditResult`)

Applying a command returns one of two implementations of the sealed interface `EditResult`:
- `EditSucceeded` — contains the updated `ApiDocument` instance and an immutable `ChangeSet` listing the changes in execution order;
- `EditRejected` — contains a non-empty list of `EditIssue` items (each with an `EditCode` and a diagnostic message).

Rejection codes (`EditCode`):
- `TARGET_NOT_FOUND` — no element with the specified `ElementId` exists in the document;
- `TARGET_TYPE_MISMATCH` — the target element exists, but its type is incompatible with the command (for example, passing the ID of a `BooleanSchema` or `SchemaComponent` instead of a `SchemaObject` when adding a property);
- `INVALID_NAME` — a component or property name (including nested properties within an added subtree) is empty or blank;
- `NAME_CONFLICT` — an element with the same name already exists in the target collection;
- `ID_CONFLICT` — the added subtree contains an `ElementId` that already exists in the document or is duplicated within the added subtree itself;
- `INVALID_STRUCTURE` — the resulting document fails `DocumentStructureValidator` checks after applying the command.

## Commands (`EditCommand`) and Changes (`Change`)

Supported schema editing commands:
- `AddSchemaComponent` — appends a new schema to `components/schemas` and emits `SchemaComponentAdded`;
- `AddSchemaProperty` — locates the target `SchemaObject` by its `ElementId` (whether a top-level component schema or a nested inline schema), appends a new property, and emits `SchemaPropertyAdded`;
- `RenameSchemaComponent` — locates a schema component by its `ElementId`, renames it, and automatically rewrites all applicable local references across the document that point to that component or its descendants.

When a schema component is renamed, the `ChangeSet` records:
- `SchemaComponentRenamed` — the component's `ElementId`, old name, and new name;
- `ReferenceChanged` — one entry per updated reference (recording `sourceBefore` and `sourceAfter` paths, old and new string values, and `ReferenceKind`). If the new name equals the current name, an isolated copy of the document and an empty `ChangeSet` are returned.

## Reference Rewriting (`ReferenceOwner`)

All model nodes that can hold direct references (`SchemaObject`, `Discriminator`, `NamedComponent`, `Operation`, `PathItem`, `ApiResponse`, `Parameter`, `Header`, `MediaType`, `Encoding`) implement `ReferenceOwner`.

When renaming a schema component:
- local fragment pointers targeting `#/components/schemas/<OldName>` or nested paths `#/components/schemas/<OldName>/...` are rewritten to `<NewName>` with proper JSON Pointer and URI escaping;
- short schema names in `Discriminator.mapping` matching the old name are replaced with the new name;
- rewritten `ObjectReference` instances preserve their `ElementId`, `summary`, and `description`;
- external references, unsupported anchors, malformed pointers, indirect references (reaching the schema through an intermediate alias), and literal payloads inside `DocumentValue` (`example`, `default`) remain untouched.
