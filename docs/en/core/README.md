# `core` Module

[Русский](../../ru/core/README.md)

The `core` module provides the internal semantic model of an API document, along with navigation, local reference resolution, and atomic command-based editing.

The module is written in pure Java with no external runtime dependencies and is independent of serialization formats (YAML/JSON) or any specific minor version of the OpenAPI 3.x specification.

## Design Principles

- **Stable identity.** Key document elements have their own internal identifier (`ElementId` based on UUID) that is independent of their name or location in the document and is preserved across renames.
- **Controlled mutation.** Models are safe to read from the outside: all collections are exposed as read-only views, and constructors and builders create deep defensive copies. Mutating an existing document is only possible inside an active editor session.
- **Draft-friendly structure.** Structural validation enforces only object tree integrity, identifier uniqueness, and name uniqueness within the same scope. Incomplete or semantically unfinished definitions are allowed so that documents can be built and edited incrementally.

## Documentation Sections

- [Domain Model](model.md) — document structure, compositional schema model, references, and data values.
- [Construction and Validation](construction.md) — building objects, deep copying, tree traversal, and structural invariants.
- [Navigation and References](references.md) — JSON Pointer addressing, document indexing, and local `$ref` resolution.
- [Document Editing](editing.md) — applying commands, mutation access control, reference rewriting, and `ChangeSet` structure.
