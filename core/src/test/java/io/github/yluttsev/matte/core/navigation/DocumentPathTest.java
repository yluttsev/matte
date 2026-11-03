package io.github.yluttsev.matte.core.navigation;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPathTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/components/schemas/User", "/a~1b/~0/~01", "/a//"})
    void testParse_WhenPointerIsValid_ShouldRoundTripWithoutLosingSegments(String pointer) {
        assertEquals(pointer, DocumentPath.parse(pointer).toString());
    }

    @Test
    void testParse_WhenSegmentsContainEscapes_ShouldDecodeExactlyOnce() {
        assertEquals(List.of("a/b", "~", "~1"), DocumentPath.parse("/a~1b/~0/~01").segments());
    }

    @ParameterizedTest
    @ValueSource(strings = {"components/schemas/User", "#/a", "/a~", "/a~2", "/~~"})
    void testParse_WhenPointerIsMalformed_ShouldRejectPointer(String pointer) {
        assertThrows(IllegalArgumentException.class, () -> DocumentPath.parse(pointer));
    }

    @Test
    void testFromFragment_WhenFragmentIsEncoded_ShouldDecodeUtf8AndPreservePlus() {
        assertEquals(List.of("schemas", "имя +%"),
                DocumentPath.fromFragment("#/schemas/%D0%B8%D0%BC%D1%8F%20+%25").segments());
        assertEquals(List.of("a+b"), DocumentPath.fromFragment("#/a%2Bb").segments());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a", "#anchor", "#/%", "#/%GG", "#/%FF", "#/%C3", "#/a~2", "#/%ＦＦ"})
    void testFromFragment_WhenFragmentIsMalformed_ShouldRejectFragment(String fragment) {
        assertThrows(IllegalArgumentException.class, () -> DocumentPath.fromFragment(fragment));
    }

    @Test
    void testToFragment_WhenSegmentsContainReservedCharacters_ShouldRoundTrip() {
        DocumentPath path = new DocumentPath(List.of("components", "schemas", "a/b~ c%+#имя"));

        assertEquals(path, DocumentPath.fromFragment(path.toFragment()));
        assertTrue(path.toFragment().contains("%25"));
        assertTrue(path.toFragment().contains("%23"));
    }

    @Test
    void testParent_WhenPathIsRootOrNested_ShouldReturnExpectedParent() {
        assertTrue(DocumentPath.root().parent().isEmpty());
        assertEquals("#", DocumentPath.root().toFragment());
        assertEquals(DocumentPath.root(), DocumentPath.parse("/components").parent().orElseThrow());
        assertEquals(
                DocumentPath.parse("/components"),
                DocumentPath.parse("/components/schemas").parent().orElseThrow()
        );
    }

    @Test
    void testStartsWith_WhenNamesShareTextPrefix_ShouldCompareWholeSegments() {
        DocumentPath user = DocumentPath.parse("/components/schemas/User");

        assertTrue(user.child("properties").child("name").startsWith(user));
        assertTrue(user.startsWith(user));
        assertTrue(user.startsWith(DocumentPath.root()));
        assertFalse(DocumentPath.parse("/components/schemas/User2").startsWith(user));
        assertFalse(DocumentPath.parse("/components/schemas").startsWith(user));
    }

    @Test
    void testConstructor_WhenInputListChanges_ShouldPreserveReadOnlySnapshot() {
        List<String> segments = new ArrayList<>(List.of("a"));
        DocumentPath path = new DocumentPath(segments);
        segments.clear();

        assertEquals("/a", path.toString());
        assertThrows(UnsupportedOperationException.class, path.segments()::clear);
    }
}
