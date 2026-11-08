# OpenAPI import

[Русский](../../ru/openapi/import.md)

Call `OpenApiImporter.importDocument(bytes, options)`, using `ImportOptions.autoDetect()` or an explicit `withFormat(...)` hint. The importer retains the original bytes, checks UTF-8 and syntax, detects duplicate keys, and accepts OpenAPI 3.0.x and 3.1.x. Scalar inputs can require an explicit format. `ImportResult` exposes the status, diagnostics, detected format and version when available, and an optional document. `IMPORTED` means a model was produced; parser diagnostics can still be present and do not imply full OpenAPI validation. An unsuccessful import retains the source but has no editable model. Returned source bytes and model snapshots are defensive copies.

The current mapping covers selected `info` fields and schema components, including supported nested schema fields and values. It does not yet map paths, operations or all other OpenAPI objects. Source preservation does not mean these sections are represented in `core` or validated completely. External references are not resolved or fetched. Some inputs, including schemas the underlying parser fails to interpret, are rejected with a mapping diagnostic.

Schema `default`, `example`, `examples`, `enum` and `const` values retain the decimal precision of JSON and YAML numbers, including numbers nested in arrays and objects. The six integer constraints `minProperties`, `maxProperties`, `minItems`, `maxItems`, `minLength` and `maxLength` are read from the source as `BigInteger`, so values above `Integer.MAX_VALUE` are retained in the model after import and editing. This does not add validation of negative or non-integer constraint values; the underlying parser's existing handling still applies to values that are not decimal integers.
