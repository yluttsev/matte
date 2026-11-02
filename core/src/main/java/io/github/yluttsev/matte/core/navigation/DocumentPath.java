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

    public DocumentPath {
        segments = List.copyOf(segments);
    }

    public static DocumentPath root() {
        return new DocumentPath(List.of());
    }

    public static DocumentPath parse(String pointer) {
        Objects.requireNonNull(pointer, "pointer");
        if (pointer.isEmpty()) {
            return root();
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("JSON Pointer must be empty or start with '/'");
        }
        List<String> segments = new ArrayList<>();
        for (String segment : pointer.substring(1).split("/", -1)) {
            segments.add(unescape(segment));
        }
        return new DocumentPath(segments);
    }

    public static DocumentPath fromFragment(String fragment) {
        Objects.requireNonNull(fragment, "fragment");
        if (!fragment.startsWith("#")) {
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
        return segments.stream().map(segment -> segment.replace("~", "~0").replace("/", "~1"))
                .collect(Collectors.joining("/", segments.isEmpty() ? "" : "/", ""));
    }

    private static String unescape(String segment) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < segment.length(); index++) {
            char current = segment.charAt(index);
            if (current != '~') {
                result.append(current);
            } else {
                if (++index >= segment.length() || (segment.charAt(index) != '0' && segment.charAt(index) != '1')) {
                    throw new IllegalArgumentException("Invalid JSON Pointer escape");
                }
                result.append(segment.charAt(index) == '0' ? '~' : '/');
            }
        }
        return result.toString();
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
                if (index + 2 >= value.length()) {
                    throw new IllegalArgumentException("Incomplete percent escape");
                }
                int high = Character.digit(value.charAt(index + 1), 16);
                int low = Character.digit(value.charAt(index + 2), 16);
                if (high < 0 || low < 0 || value.charAt(index + 1) > 127 || value.charAt(index + 2) > 127) {
                    throw new IllegalArgumentException("Invalid percent escape");
                }
                bytes.write(high * 16 + low);
                index += 3;
            }
            try {
                result.append(StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes.toByteArray())));
            } catch (CharacterCodingException exception) {
                throw new IllegalArgumentException("Invalid UTF-8 in URI fragment", exception);
            }
        }
        return result.toString();
    }
}
