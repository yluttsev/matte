package io.github.yluttsev.matte.openapi.parser;

import io.github.yluttsev.matte.openapi.ImportOptions;
import org.snakeyaml.engine.v2.api.LoadSettings;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class ParserSettings {

    private static final LoadSettings YAML = LoadSettings.builder()
            .setCodePointLimit(ImportOptions.MAX_SUPPORTED_SOURCE_BYTES).build();
    private static final JsonMapper JSON = createJsonMapper();

    private ParserSettings() {
    }

    public static LoadSettings yaml() {
        return YAML;
    }

    public static JsonMapper json() {
        return JSON;
    }

    private static JsonMapper createJsonMapper() {
        JsonFactory factory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(ImportOptions.MAX_SUPPORTED_SOURCE_BYTES).build())
                .build();
        return JsonMapper.builder(factory)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();
    }
}
