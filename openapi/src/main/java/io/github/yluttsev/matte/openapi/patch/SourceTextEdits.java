package io.github.yluttsev.matte.openapi.patch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.ObjectMapper;

final class SourceTextEdits {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SAFE_PLAIN_SCALAR = Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]*");
    private static final Set<String> YAML_KEYWORDS = Set.of("true", "false", "null");

    private final List<TextEdit> edits = new ArrayList<>();

    void replace(SourceScalar scalar, String value) {
        edits.add(new TextEdit(scalar.start(), scalar.end(), formatValue(scalar.style(), value)));
    }

    void remove(int start, int end) {
        edits.add(new TextEdit(start, end, ""));
    }

    static String yamlScalar(String value) {
        return formatValue(SourceScalar.QuoteStyle.PLAIN, value);
    }

    String apply(String text) {
        edits.sort(Comparator.comparingInt(TextEdit::start).reversed());
        StringBuilder result = new StringBuilder(text);
        int previousStart = text.length();
        for (TextEdit edit : edits) {
            if (edit.start() < 0 || edit.end() > previousStart || edit.start() >= edit.end()) {
                throw new IllegalArgumentException("Source edits overlap or have invalid positions");
            }
            result.replace(edit.start(), edit.end(), edit.replacement());
            previousStart = edit.start();
        }
        return result.toString();
    }

    private static String formatValue(SourceScalar.QuoteStyle style, String value) {
        if (style == SourceScalar.QuoteStyle.SINGLE_QUOTED) {
            return "'" + value.replace("'", "''") + "'";
        }
        if (style == SourceScalar.QuoteStyle.PLAIN && isSafePlainScalar(value)) {
            return value;
        }
        return JSON.writeValueAsString(value);
    }

    private static boolean isSafePlainScalar(String value) {
        return SAFE_PLAIN_SCALAR.matcher(value).matches()
                && !YAML_KEYWORDS.contains(value.toLowerCase(Locale.ROOT));
    }

    private record TextEdit(int start, int end, String replacement) {
    }
}
