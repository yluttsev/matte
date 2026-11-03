# Navigation and References (`core.navigation`, `core.reference`)

[Русский](../../ru/core/references.md)

These two packages provide node addressing via JSON Pointer (RFC 6901), element indexing by path and identifier, and resolution and tracking of local `$ref` references.

## Node Addressing (`DocumentPath`)

`DocumentPath` represents the location of a node in the document as an immutable list of unescaped string segments and supports two text formats:
- **JSON Pointer** (`parse("/paths/~1users/get")` and `toString()`) — escapes only `~` (`~0`) and `/` (`~1`);
- **URI fragment** (`fromFragment("#/components/schemas/User")` and `toFragment()`) — additionally applies UTF-8 percent-encoding/decoding for URI-reserved characters (spaces, `%`, `#`, `+`, and non-ASCII characters).

Subtree prefix checks use `startsWith(prefix)`, which compares whole path segments rather than raw string prefixes (so `/components/schemas/User2` does not match `/components/schemas/User`).

## Document Index (`DocumentIndex`)

`DocumentIndex.build(document)` walks the document tree and builds a lookup index:
- `findById(id)` — looks up an element and its logical path by its internal `ElementId`;
- `findByPath(path)` — looks up the target object at a given `DocumentPath`.

Path mapping rules:
- paths follow OpenAPI document structure rather than internal Java field names (for example, the `GET` operation under `/users` is indexed at `/paths/~1users/get`, without an intermediate `operations` segment);
- named element wrappers (`SchemaComponent`, `SchemaProperty`, `NamedComponent`, `InlineObject`) are path-transparent and share the same `DocumentPath` with the object they wrap;
- `findById` can look up either the wrapper (by its ID) or the inner object (by its ID), and both report the same `DocumentPath`;
- `findByPath` returns the inner target object (such as `Schema` or `Parameter`) that `$ref` pointers resolve to.

## Reference Resolution (`ReferenceResolver`)

The resolver takes a document index, a reference string, and the expected target type, returning a `ReferenceResolution<T>`:
- `ResolvedReference<T>` — holds the resolved typed object, its final `targetPath`, and the list of all `traversedPaths`;
- `UnresolvedReference<T>` — holds the failure reason (`ReferenceFailure`) and the list of `traversedPaths` visited before the failure occurred.

If a reference points to a component alias (for example, a parameter in `components/parameters` that is itself an `ObjectReference` to another parameter), the resolver follows the chain to the final target and records every intermediate path in `traversedPaths`. A schema that references itself (`$ref` inside `SchemaObject`) resolves directly to that schema without infinite expansion.

Unresolved reference reasons (`ReferenceFailure`):
- `INVALID_REFERENCE` — malformed URI syntax, invalid percent-escape, or invalid `~` escape sequence in JSON Pointer;
- `EXTERNAL_REFERENCE` — reference includes a URI scheme (`http`/`https`), authority, query, or file path;
- `UNSUPPORTED_ANCHOR` — fragment does not start with `/` (such as a JSON Schema anchor `#User` instead of a JSON Pointer `#/components/schemas/User`);
- `TARGET_NOT_FOUND` — no node exists at the target path in the document;
- `TYPE_MISMATCH` — a node exists at the target path, but its type does not match what the reference location expects;
- `CYCLIC_REFERENCE` — a chain of component aliases forms a cycle.

## Reference Usage Index (`ReferenceIndex`)

`ReferenceIndexBuilder.build(documentIndex)` collects all reference usages across the document into a `ReferenceIndex`:
- schema references (`ReferenceKind.SCHEMA`) — the `$ref` field on `SchemaObject`;
- object references (`ReferenceKind.OBJECT`) — `ObjectReference` instances in parameters, request bodies, responses, headers, examples, and components;
- discriminator mappings (`ReferenceKind.DISCRIMINATOR_MAPPING`) — entries in `Discriminator.mapping`, supporting both full `#/components/schemas/...` pointers and short names of existing schemas.

Literal `DocumentValue` payloads (inside `example` and `default`) are never indexed as references, even if they contain a `"$ref"` key.

Key lookup methods on `ReferenceIndex`:
- `usages()` and `unresolvedUsages()` — all references in the document and the subset of unresolved references;
- `findBySource(path)` — finds a reference usage by the path of its `$ref` field (or `mapping` entry);
- `referencesTo(id)` and `referencesTo(path)` — all incoming references pointing to the given element (including references that pass through it via an alias chain);
- `referencesToSubtree(path)` and `referencesFromSubtree(path)` — all incoming references targeting a subtree and all outgoing references originating from a subtree.
