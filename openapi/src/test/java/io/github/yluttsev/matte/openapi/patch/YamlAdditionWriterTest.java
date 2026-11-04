package io.github.yluttsev.matte.openapi.patch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YamlAdditionWriterTest {

    private final YamlAdditionWriter writer = new YamlAdditionWriter();

    @Test
    void testAdd_WhenTargetUsesAlias_ShouldRejectAmbiguousInsertion() throws IOException {
        String text = fixture("source-ranges-alias.yaml");
        DocumentPath alias = DocumentPath.schemaComponent("Alias");

        assertThrows(IllegalArgumentException.class, () -> writer.add(text, alias.child("properties"),
                alias, "name", Map.of("type", "string")));
    }

    @Test
    void testAdd_WhenLastEntryUsesAlias_ShouldRejectAmbiguousInsertion() throws IOException {
        String text = fixture("source-ranges-alias.yaml");
        DocumentPath schemas = DocumentPath.parse("/components/schemas");

        assertThrows(IllegalArgumentException.class,
                () -> writer.add(text, schemas, schemas, "New", Map.of("type", "object")));
    }

    @Test
    void testAdd_WhenTargetUsesMergeKey_ShouldRejectAmbiguousInsertion() throws IOException {
        String text = fixture("source-ranges-merge.yaml");
        DocumentPath old = DocumentPath.schemaComponent("Old");

        assertThrows(IllegalArgumentException.class, () -> writer.add(text, old.child("properties"),
                old, "name", Map.of("type", "string")));
    }

    @Test
    void testAdd_WhenBlockHasComments_ShouldPreserveBothComments() throws IOException {
        String text = fixture("source-ranges-comments.yaml");
        DocumentPath schemas = DocumentPath.parse("/components/schemas");

        String actual = writer.add(text, schemas, schemas, "New", Map.of("type", "object"));

        assertEquals(text.replace("# keep tail comment", "    New:\n      type: object\n# keep tail comment"),
                actual);
    }

    @Test
    void testAdd_WhenFlowHasNonBmpCharacter_ShouldPreserveUnchangedText() throws IOException {
        String text = fixture("source-ranges-flow.yaml");
        DocumentPath schemas = DocumentPath.parse("/components/schemas");

        String actual = writer.add(text, schemas, schemas, "New", Map.of("type", "object"));

        assertEquals(text.replace("{Old: {type: object}}",
                "{Old: {type: object}, \"New\":{\"type\":\"object\"}}"), actual);
    }

    @Test
    void testAdd_WhenBlockHasNonBmpCharacter_ShouldUseUtf16InsertionOffset() throws IOException {
        String text = fixture("source-ranges-unicode.yaml");
        DocumentPath schemas = DocumentPath.parse("/components/schemas");

        String actual = writer.add(text, schemas, schemas, "New", Map.of("type", "object"));

        assertEquals(text + "    New:\n      type: object\n", actual);
    }

    @Test
    void testAdd_WhenLineEndingsAreMixed_ShouldUseNearbyStyle() throws IOException {
        String text = fixture("source-ranges-comments.yaml").replaceFirst("\n", "\r\n");
        DocumentPath schemas = DocumentPath.parse("/components/schemas");

        String actual = writer.add(text, schemas, schemas, "New", Map.of("type", "object"));

        assertEquals(text.replace("# keep tail comment", "    New:\n      type: object\n# keep tail comment"),
                actual);
    }

    private String fixture(String name) throws IOException {
        return new String(TestFixtures.bytes(name), StandardCharsets.UTF_8);
    }
}
