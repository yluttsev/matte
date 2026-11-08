package io.github.yluttsev.matte.openapi;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.openapi.testing.TestFixtures;
import io.swagger.v3.parser.util.ClasspathHelper;
import io.swagger.v3.parser.util.RemoteUrl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OfflineImportTest {

    @ParameterizedTest
    @ValueSource(strings = {"offline-refs-3.0.yaml", "offline-refs-3.1.yaml"})
    void testImportDocument_WhenRefsAreExternal_ShouldNotAccessNetworkOrFiles(String fixture) throws IOException {
        byte[] content = TestFixtures.bytes(fixture);
        OpenApiImporter importer = new OpenApiImporter();
        importer.importDocument(TestFixtures.bytes("minimal.json"), ImportOptions.autoDetect());

        try (MockedStatic<RemoteUrl> network = Mockito.mockStatic(RemoteUrl.class);
                MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                MockedStatic<ClasspathHelper> classpath = Mockito.mockStatic(ClasspathHelper.class)) {
            ImportResult result = importer.importDocument(content, ImportOptions.autoDetect());

            assertEquals(ImportStatus.IMPORTED, result.status(), result.diagnostics().toString());
            assertArrayEquals(content, result.source().content());
            var schemas = result.document().orElseThrow().getComponents().getSchemas();
            assertEquals(2, schemas.size());
            List<String> references = schemas.stream()
                    .map(component -> assertInstanceOf(SchemaObject.class, component.getSchema()))
                    .map(schema -> schema.getReference().value())
                    .toList();
            assertEquals(List.of(
                    "http://127.0.0.1:1/remote.yaml#/components/schemas/Item",
                    "./offline-neighbor.yaml#/components/schemas/Item"
            ), references);
            network.verifyNoInteractions();
            files.verifyNoInteractions();
            classpath.verifyNoInteractions();
        }
    }
}
