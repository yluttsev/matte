package io.github.yluttsev.matte.openapi.patch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonAdditionWriterTest {

    private final JsonAdditionWriter writer = new JsonAdditionWriter();

    @Test
    void testAdd_WhenJsonHasBomAndNonBmpCharacter_ShouldUseUtf16InsertionOffset() throws IOException {
        String text = Character.toString((char) 0xFEFF) + fixture("source-ranges-unicode.json");
        DocumentPath schemas = DocumentPath.parse("/components/schemas");

        String actual = writer.add(text, schemas, schemas, "New", Map.of("type", "object"));

        assertEquals(text.replace("\"Old\":{\"type\":\"object\"}",
                "\"Old\":{\"type\":\"object\"},\"New\":{\"type\":\"object\"}"), actual);
    }

    @Test
    void testAdd_WhenJsonLineEndingsAreMixed_ShouldUseNearbyStyle() throws IOException {
        String text = fixture("additions-formatted.json").replaceFirst("\n", "\r\n");
        DocumentPath properties = DocumentPath.parse("/components/schemas/Customer/properties");

        String actual = writer.add(text, properties, properties, "email", Map.of("type", "string"));

        assertEquals(text.replace("\"id\": {\"type\": \"string\"}",
                "\"id\": {\"type\": \"string\"},\n          \"email\": {\"type\":\"string\"}"), actual);
    }

    private String fixture(String name) throws IOException {
        return new String(TestFixtures.bytes(name), StandardCharsets.UTF_8);
    }
}
