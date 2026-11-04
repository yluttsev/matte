package io.github.yluttsev.matte.openapi.patch;

final class SourceLineEndings {

    private SourceLineEndings() {
    }

    static String near(String text, int position) {
        int previous = text.lastIndexOf('\n', position - 1);
        int next = text.indexOf('\n', position);
        int newline = previous >= 0 ? previous : next;
        return newline > 0 && text.charAt(newline - 1) == '\r' ? "\r\n" : "\n";
    }
}
