# OpenAPI import and source editing

[Русский](../../ru/openapi/README.md)

The `openapi` module accepts document bytes and interprets YAML or JSON as OpenAPI. It depends on `core`; loading files, fetching URLs and saving documents belong to the caller. The source is retained independently of the partial semantic model, so unsupported document sections are not discarded on import.

- [Import and model mapping](import.md)
- [Editing results, refusals and source guarantees](editing.md)
- [Reference locations](references.md)
