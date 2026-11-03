package io.github.yluttsev.matte.core.navigation;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record DocumentPath(List<String> segments) {

    private static final String POINTER_SEPARATOR = "/";
    private static final String FRAGMENT_PREFIX = "#";
    private static final String ESCAPED_TILDE = "~0";
    private static final String ESCAPED_SLASH = "~1";
    private static final DocumentPath SCHEMAS_PATH = new DocumentPath(List.of("components", "schemas"));

    public DocumentPath {
        segments = List.copyOf(segments);
    }

    public static DocumentPath root() {
        return new DocumentPath(List.of());
    }

    public static DocumentPath schemaComponent(String name) {
        return SCHEMAS_PATH.child(Objects.requireNonNull(name, "name"));
    }

    public static DocumentPath parse(String pointer) {
        Objects.requireNonNull(pointer, "pointer");
        if (pointer.isEmpty()) {
            return root();
        }
        if (!pointer.startsWith(POINTER_SEPARATOR)) {
            throw new IllegalArgumentException("JSON Pointer must be empty or start with '/'");
        }
        List<String> segments = new ArrayList<>();
        for (String segment : pointer.substring(1).split(POINTER_SEPARATOR, -1)) {
            segments.add(unescape(segment));
        }
        return new DocumentPath(segments);
    }

    public static DocumentPath fromFragment(String fragment) {
        Objects.requireNonNull(fragment, "fragment");
        if (!fragment.startsWith(FRAGMENT_PREFIX)) {
            throw new IllegalArgumentException("URI fragment must start with '#'");
        }
        return parse(decode(fragment.substring(1)));
    }

    public DocumentPath child(String segment) {
        List<String> result = new ArrayList<>(segments);
        result.add(Objects.requireNonNull(segment, "segment"));
        return new DocumentPath(result);
    }

    public Optional<DocumentPath> parent() {
        return segments.isEmpty() ? Optional.empty()
                : Optional.of(new DocumentPath(segments.subList(0, segments.size() - 1)));
    }

    public boolean startsWith(DocumentPath prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return segments.size() >= prefix.segments.size()
                && segments.subList(0, prefix.segments.size()).equals(prefix.segments);
    }

    public String toFragment() {
        try {
            return new URI(null, null, toString()).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Cannot encode document path", exception);
        }
    }

    @Override
    public String toString() {
        return segments.stream()
                .map(DocumentPath::escape)
                .collect(Collectors.joining(POINTER_SEPARATOR, segments.isEmpty() ? "" : POINTER_SEPARATOR, ""));
    }

    private static String escape(String segment) {
        return segment.replace("~", ESCAPED_TILDE).replace(POINTER_SEPARATOR, ESCAPED_SLASH);
    }

    private static String unescape(String segment) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < segment.length(); index++) {
            char current = segment.charAt(index);
            if (current != '~') {
                result.append(current);
            } else {
                index++;
                result.append(unescapeToken(segment, index));
            }
        }
        return result.toString();
    }

    private static char unescapeToken(String segment, int index) {
        if (index >= segment.length()) {
            throw new IllegalArgumentException("Invalid JSON Pointer escape");
        }
        char token = segment.charAt(index);
        if (token == '0') {
            return '~';
        }
        if (token == '1') {
            return '/';
        }
        throw new IllegalArgumentException("Invalid JSON Pointer escape");
    }

    private static String decode(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length();) {
            if (value.charAt(index) != '%') {
                result.append(value.charAt(index++));
                continue;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (index < value.length() && value.charAt(index) == '%') {
                bytes.write(decodePercentByte(value, index));
                index += 3;
            }
            result.append(decodeUtf8(bytes.toByteArray()));
        }
        return result.toString();
    }

    private static int decodePercentByte(String value, int index) {
        if (index + 2 >= value.length()) {
            throw new IllegalArgumentException("Incomplete percent escape");
        }
        int high = parseAsciiHexDigit(value.charAt(index + 1));
        int low = parseAsciiHexDigit(value.charAt(index + 2));
        return high * 16 + low;
    }

    private static int parseAsciiHexDigit(char character) {
        int digit = character <= 127 ? Character.digit(character, 16) : -1;
        if (digit < 0) {
            throw new IllegalArgumentException("Invalid percent escape");
        }
        return digit;
    }

    private static CharSequence decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Invalid UTF-8 in URI fragment", exception);
        }
    }
}
