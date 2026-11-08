# OpenAPI editing

[Русский](../../ru/openapi/editing.md)

`OpenApiEditor` supports schema component rename/add/delete, property add/rename/delete, and schema-object update. Use `apply(state, command)` or the command-specific methods (`rename`, `addComponent`, `addProperty`, `renameProperty`, `deleteComponent`, `deleteProperty`, `updateSchema`). All methods return the sealed `OpenApiEditResult`; the old `OpenApiSchemaEditor` and `RenameResult` have been removed.

The default constructor supplies `DocumentEditor` and `OpenApiImporter`. `OpenApiEditor(DocumentEditor, OpenApiImporter)` uses the supplied instances for semantic execution and re-import, allowing a composition root to supply its dependencies.

## Result contract

| Result | Payload | State and source |
| --- | --- | --- |
| `Applied` | `edit()` is `EditSucceeded`; `updatedSource()` is the new source | `state()` is the new imported state; `source()` is present |
| `Rejected` | `edit()` is `EditRejected`, containing the semantic `EditIssue` list | `state()` is the exact input instance; `source()` is empty |
| `Refused` | `code()`, `message()`, `path()` (`DocumentPath`) describe a safe source-layer refusal | `state()` is the exact input instance; `source()` is empty |

Commands use identifiers from the imported model. The editor executes against a copy, patches the source, then re-imports it. It compares schema component data represented in `core`, including nested values and references, ignoring generated `ElementId` values. Sections not yet mapped, such as paths, are outside the semantic comparison. `Applied.state()` preserves the semantic identifiers and must be passed to the next operation. An unchanged rename or update preserves the original bytes. Every refusal or rejection leaves the input bytes and model unchanged; no partial source is returned. Deletion checks incoming references both in the semantic model and in source sections not yet mapped.

`null` arguments, missing constructor dependencies and a non-imported input state are caller errors (`IllegalArgumentException`). Failed re-import or disagreement between patched source and the semantic model is an internal defect (`IllegalStateException`). Expected source refusals do not escape the public editor as exceptions. Patchers use the internal `SourceEditRefusal` control-flow exception, which the editor translates to `Refused`; unrelated programming errors are not caught or reclassified. Patcher and verifier selection uses exhaustive switches over sealed `EditCommand` without a default branch, so a new command requires an explicit source implementation.

## Source refusal codes

| Code | Meaning |
| --- | --- |
| `SHARED_YAML_NODE` | The mutation path or changed subtree is shared through aliases, or an alias prevents safe insertion |
| `AMBIGUOUS_COMMENT` | A comment cannot safely be assigned to the changed or adjacent node |
| `MERGE_KEY` | The source mapping uses a YAML merge key |
| `AMBIGUOUS_LAYOUT` | A source mapping, array item, separator or boundary cannot be edited safely |
| `NESTED_REPLACEMENT` | A parent update would replace or remove an existing nested schema field |
| `SCHEMA_ID_SCOPE` | An OAS 3.1 `$id` scope prevents safe schema rename/delete |
| `UNRECOGNIZED_REFERENCE` | Rename would leave a target reference in an unrecognized position; the message lists every such position |
| `UNSUPPORTED_IN_VERSION` | The selected OAS version cannot represent the requested schema fields |

The path identifies the affected field or reference where available; otherwise it identifies the command target. For an unrecognized reference it is the first location listed in the message. A delete blocked by incoming references is `Rejected` with `REFERENCED_TARGET`, including all source locations. See [core editing](../core/editing.md) for semantic issues.

## Source preservation

Updates and structural edits patch only the selected source nodes rather than serializing the complete document. Property rename also updates matching `required` names and local pointers to that property subtree. The renamed entry keeps its position in `required`. If `required` already contains the new property name, the old entry is removed from the source array and the order of the remaining entries is preserved. When an update changes `required`, only the added or removed array items are edited: removed items are deleted in place and added items are appended with the separator and indentation of the existing array, so the rest of the array is not rewritten. A reorder alone leaves the source untouched. The edit is refused when the array is shared through a YAML alias, contains a non-string item, or has a comment next to a removed item.

The order of `required` is kept as written in the source on import, and the model comparison after an edit treats `required` as a set: the order of names is not significant for the result check, but the source order is preserved. Reference relocation uses the OpenAPI object structure, including sections not yet mapped to `core`. Literal values and extension payloads are preserved. See the [reference-location rules](references.md). A discriminator mapping key named `$ref` is treated as a mapping label, not as a reference field. Delete commands reject incoming references outside the deleted subtree and preserve empty `properties` or `schemas` sections as `{}`. To avoid discarding unknown fields or comments, an update cannot replace or remove an existing `properties`, `items`, `additionalProperties`, `allOf`, `oneOf`, `anyOf`, `not` or `discriminator` value through its parent. Target an existing nested schema directly for its field updates, or use `addProperty` to add a property. Adding a previously absent nested field is supported when representable in the selected OpenAPI version. Literal values explicitly replaced by a command are replaced as a whole.

For OpenAPI 3.1, schema component or property renames and deletions return `Refused(SCHEMA_ID_SCOPE)` when a non-literal `$id` key is found outside literal values and extension payloads. `$id` changes the base URI for relative references, while the current editor does not resolve references in those URI scopes. Because some Schema Object locations are not yet mapped, the guard conservatively rejects the whole structural edit, including references outside the `$id` subtree, and returns the original state without source. A `$anchor` alone does not change the base URI and does not trigger this refusal. OpenAPI 3.0 documents are not subject to this guard.

YAML aliases, merge keys, comments and flow mappings can make an edit ambiguous; affected operations may reject such input rather than modify it. In particular, deletion refuses ambiguous leading or trailing comments and comments between a removed block entry and its sibling; comments inside a deleted node are removed with that node. The supported source edits are narrower than the set of valid OpenAPI documents, and import is not a promise that every subsequent edit can be applied.

Before a YAML schema or property addition, `updateSchema`, or schema/property deletion, the editor detects nodes shared through aliases along the mutation path and within a replaced or removed value. It refuses those edits with `Refused(SHARED_YAML_NODE)` so an edit cannot silently change another schema or leave the imported model inconsistent. Aliases elsewhere in the document do not by themselves prevent an edit.

Removing the sole entry of a block mapping keeps the following key, its indentation and intervening blank lines. Replacing a literal (`|`) or folded (`>`) scalar, or an empty scalar such as `default:`, keeps the next key on its own line and inserts the space required after `:`. An inline comment on the block scalar indicator or empty scalar causes a safe refusal because its ownership is ambiguous. Adding a field after a multiline flow array or mapping places it after the closing `]` or `}` and preserves nearby comments and line endings.

When rename must change a reference shared through YAML aliases, it returns `Refused(SHARED_YAML_NODE)` instead of treating the missing duplicate scalar range as a caller error.
