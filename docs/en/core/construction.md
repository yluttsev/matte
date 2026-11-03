# Construction and Validation (`core.construction`)

[Русский](../../ru/core/construction.md)

This package handles safe model construction, deep copying, document tree traversal, and enforcement of core structural invariants.

## Building and Deep Copying

All mutable nodes in the domain model implement `CopyableModel<T>` and provide a `copy()` method.

Constructors and `Builder` classes follow strict isolation rules:
- input lists and maps are snapshotted while preserving element order and rejecting `null` elements;
- `build()` always returns an independent deep copy of the builder draft, so reusing a builder never mutates previously built objects;
- all collection getters return unmodifiable views;
- deep-copying a document preserves all field values, element order, and internal identifiers (`ElementId`), while instantiating a fresh object for every node in the graph.

## Model Tree Traversal (`ModelWalker`)

`ModelWalker` traverses all owned model nodes in preorder:
- `walk(root, visitor)` accepts any model node and passes each visited node along with its diagnostic path (for example, `$.components.schemas[0].schema.properties[0]`);
- `walkDocument(document, visitor)` traverses an entire document and provides each node's logical `DocumentPath`.

During traversal, strict ownership integrity is enforced:
- the same node instance cannot appear in the tree more than once (shared object references and ownership cycles are rejected with `IllegalArgumentException`);
- string `$ref` references are not followed during tree traversal.

## Structural Validation (`DocumentStructureValidator`)

The validator runs automatically during `ApiDocument.Builder.build()`, when building a document index, and after applying commands in the editor.

It enforces three structural invariants:
1. **Single ownership of every node** (no Java object instance is shared across multiple locations in the tree).
2. **Global uniqueness of `ElementId`** — no two identifiable elements anywhere in the document may share the same identifier.
3. **Name uniqueness within each collection**:
   - path strings within the document path list;
   - component names within each individual section of `Components` (identical names across different sections, such as a schema named `Same` and a parameter named `Same`, are allowed);
   - property names within a single schema object.

Semantic OpenAPI specification rules (such as required `info` or `responses` fields, or resolvable `$ref` targets) are intentionally not enforced here so that unfinished drafts remain valid structures.
