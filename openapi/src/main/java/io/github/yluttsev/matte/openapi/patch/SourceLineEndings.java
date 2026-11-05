package io.github.yluttsev.matte.openapi.patch;

final class SourceLineEndings {

    private SourceLineEndings() {
    }

    static String near(String text, int position) {
        int previous = text.lastIndexOf('\n', position - 1);
        int next = text.indexOf('\n', position);
        int newline = next;
        if (previous >= 0) {
            newline = previous;
        }
        if (newline > 0 && text.charAt(newline - 1) == '\r') {
            return "\r\n";
        }
        return "\n";
    }
}
