package io.github.yluttsev.matte.openapi.patch;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class OpenApiReferenceLocation {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "options", "head", "patch", "trace"
    );
    private static final Set<String> SINGLE_SCHEMAS = Set.of(
            "additionalProperties", "additionalItems", "contains", "else", "if", "items", "not",
            "propertyNames", "then", "unevaluatedItems", "unevaluatedProperties", "contentSchema"
    );
    private static final Set<String> SCHEMA_MAPS = Set.of(
            "$defs", "definitions", "dependentSchemas", "patternProperties", "properties"
    );
    private static final Set<String> SCHEMA_ARRAYS = Set.of("allOf", "anyOf", "oneOf", "prefixItems");
    private static final Set<String> SCHEMA_LITERALS = Set.of("default", "enum", "const", "example", "examples");
    private static final Map<String, Kind> COMPONENTS = Map.of(
            "schemas", Kind.SCHEMA, "parameters", Kind.PARAMETER, "headers", Kind.PARAMETER,
            "requestBodies", Kind.BODY, "responses", Kind.RESPONSE, "callbacks", Kind.CALLBACK,
            "pathItems", Kind.PATH_ITEM, "examples", Kind.EXAMPLE, "links", Kind.LINK,
            "securitySchemes", Kind.REFERENCE_OBJECT
    );

    private OpenApiReferenceLocation() {
    }

    static Kind classify(List<String> segments) {
        Kind kind = Kind.ROOT;
        Kind entry = Kind.UNKNOWN;
        for (String segment : segments) {
            if (kind == Kind.NAMED_MAP) {
                kind = entry;
            } else if (kind == Kind.ARRAY) {
                if (isArrayIndex(segment)) {
                    kind = entry;
                } else {
                    kind = Kind.UNKNOWN;
                }
            } else {
                Transition transition = child(kind, segment);
                kind = transition.kind();
                entry = transition.entry();
            }
        }
        return kind;
    }

    private static Transition child(Kind parent, String field) {
        if (parent == Kind.OPAQUE || parent == Kind.UNKNOWN) {
            return node(parent);
        }
        if (parent == Kind.MAPPING) {
            return node(Kind.MAPPING_VALUE);
        }
        if (field.startsWith("x-")) {
            return node(Kind.OPAQUE);
        }
        if (field.equals("$ref") && isReferenceObject(parent)) {
            return node(Kind.REFERENCE);
        }
        return switch (parent) {
            case ROOT -> rootChild(field);
            case PATHS -> node(Kind.PATH_ITEM);
            case COMPONENTS -> map(COMPONENTS.getOrDefault(field, Kind.UNKNOWN));
            case PATH_ITEM -> pathItemChild(field);
            case OPERATION -> operationChild(field);
            case PARAMETER -> parameterChild(field);
            case BODY -> named(field, "content", Kind.MEDIA);
            case RESPONSE -> responseChild(field);
            case MEDIA -> mediaChild(field);
            case ENCODING -> named(field, "headers", Kind.PARAMETER);
            case CALLBACK -> node(Kind.PATH_ITEM);
            case SCHEMA -> schemaChild(field);
            case DISCRIMINATOR -> fieldNode(field, "mapping", Kind.MAPPING);
            case EXAMPLE -> fieldNode(field, "value", Kind.OPAQUE);
            case LINK -> linkChild(field);
            default -> node(Kind.UNKNOWN);
        };
    }

    private static Transition rootChild(String field) {
        return switch (field) {
            case "components" -> node(Kind.COMPONENTS);
            case "paths" -> node(Kind.PATHS);
            case "webhooks" -> map(Kind.PATH_ITEM);
            default -> node(Kind.UNKNOWN);
        };
    }

    private static Transition pathItemChild(String field) {
        if (HTTP_METHODS.contains(field)) {
            return node(Kind.OPERATION);
        }
        if (field.equals("parameters")) {
            return new Transition(Kind.ARRAY, Kind.PARAMETER);
        }
        return node(Kind.UNKNOWN);
    }

    private static Transition operationChild(String field) {
        return switch (field) {
            case "parameters" -> new Transition(Kind.ARRAY, Kind.PARAMETER);
            case "requestBody" -> node(Kind.BODY);
            case "responses" -> map(Kind.RESPONSE);
            case "callbacks" -> map(Kind.CALLBACK);
            default -> node(Kind.UNKNOWN);
        };
    }

    private static Transition parameterChild(String field) {
        return switch (field) {
            case "schema" -> node(Kind.SCHEMA);
            case "content" -> map(Kind.MEDIA);
            case "examples" -> map(Kind.EXAMPLE);
            case "example" -> node(Kind.OPAQUE);
            default -> node(Kind.UNKNOWN);
        };
    }

    private static Transition responseChild(String field) {
        return switch (field) {
            case "content" -> map(Kind.MEDIA);
            case "headers" -> map(Kind.PARAMETER);
            case "links" -> map(Kind.LINK);
            default -> node(Kind.UNKNOWN);
        };
    }

    private static Transition mediaChild(String field) {
        return switch (field) {
            case "schema" -> node(Kind.SCHEMA);
            case "encoding" -> map(Kind.ENCODING);
            case "examples" -> map(Kind.EXAMPLE);
            case "example" -> node(Kind.OPAQUE);
            default -> node(Kind.UNKNOWN);
        };
    }

    private static Transition schemaChild(String field) {
        if (SCHEMA_LITERALS.contains(field)) {
            return node(Kind.OPAQUE);
        }
        if (SINGLE_SCHEMAS.contains(field)) {
            return node(Kind.SCHEMA);
        }
        if (SCHEMA_MAPS.contains(field)) {
            return map(Kind.SCHEMA);
        }
        if (SCHEMA_ARRAYS.contains(field)) {
            return new Transition(Kind.ARRAY, Kind.SCHEMA);
        }
        return fieldNode(field, "discriminator", Kind.DISCRIMINATOR);
    }

    private static Transition linkChild(String field) {
        if (field.equals("parameters") || field.equals("requestBody")) {
            return node(Kind.OPAQUE);
        }
        return node(Kind.UNKNOWN);
    }

    private static boolean isReferenceObject(Kind kind) {
        return switch (kind) {
            case SCHEMA, PATH_ITEM, PARAMETER, BODY, RESPONSE, CALLBACK, EXAMPLE, LINK, REFERENCE_OBJECT -> true;
            default -> false;
        };
    }

    private static Transition named(String field, String expected, Kind entry) {
        if (field.equals(expected)) {
            return map(entry);
        }
        return node(Kind.UNKNOWN);
    }

    private static Transition fieldNode(String field, String expected, Kind kind) {
        if (field.equals(expected)) {
            return node(kind);
        }
        return node(Kind.UNKNOWN);
    }

    private static Transition map(Kind entry) {
        return new Transition(Kind.NAMED_MAP, entry);
    }

    private static Transition node(Kind kind) {
        return new Transition(kind, Kind.UNKNOWN);
    }

    private static boolean isArrayIndex(String segment) {
        return !segment.isEmpty() && segment.chars().allMatch(Character::isDigit);
    }

    enum Kind {
        ROOT, COMPONENTS, PATHS, PATH_ITEM, OPERATION, PARAMETER, BODY, RESPONSE, MEDIA, ENCODING, CALLBACK,
        SCHEMA, DISCRIMINATOR, MAPPING, MAPPING_VALUE, EXAMPLE, LINK, REFERENCE_OBJECT,
        NAMED_MAP, ARRAY, REFERENCE, OPAQUE, UNKNOWN
    }

    private record Transition(Kind kind, Kind entry) {
    }
}
