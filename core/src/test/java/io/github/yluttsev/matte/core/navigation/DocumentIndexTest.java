package io.github.yluttsev.matte.core.navigation;

import io.github.yluttsev.matte.core.construction.ModelFixture;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.Components;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.IdentifiedElement;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.path.HttpMethod;
import io.github.yluttsev.matte.core.model.schema.BooleanSchema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentIndexTest {

    @Test
    void testFindById_WhenElementExists_ShouldReturnElementAndNamedPath() {
        SchemaObject schema = SchemaObject.builder(ElementId.random()).properties(List.of(
                new SchemaProperty(ElementId.random(), "a/b~", new BooleanSchema(ElementId.random(), true)))).build();
        SchemaComponent component = new SchemaComponent(ElementId.random(), "User", schema);
        ApiDocument document = ApiDocument.builder(ElementId.random()).components(
                Components.builder().schemas(List.of(component)).build()).build();
        DocumentIndex index = DocumentIndex.build(document);

        assertEquals(DocumentPath.parse("/components/schemas/User"), index.findById(component.getId()).orElseThrow().path());
        assertEquals(DocumentPath.parse("/components/schemas/User"), index.findById(schema.getId()).orElseThrow().path());
        DocumentEntry property = index.findById(schema.getProperties().getFirst().getId()).orElseThrow();
        assertEquals(DocumentPath.parse("/components/schemas/User/properties/a~1b~0"), property.path());
        assertInstanceOf(SchemaProperty.class, property.value());
        assertInstanceOf(BooleanSchema.class, index.findByPath(property.path()).orElseThrow().value());
    }

    @Test
    void testFindByPath_WhenDocumentContainsOperationsAndResponses_ShouldUseOpenApiLocations() {
        ApiDocument document = ModelFixture.document();
        DocumentIndex index = DocumentIndex.build(document);

        assertSame(document.getPaths().getFirst().getOperations().get(HttpMethod.GET),
                index.findByPath(DocumentPath.parse("/paths/~1users/get")).orElseThrow().value());
        assertInstanceOf(MediaType.class, index.findByPath(DocumentPath.parse(
                "/paths/~1users/get/responses/200/content/application~1json")).orElseThrow().value());
        assertTrue(index.findByPath(DocumentPath.parse("/paths/~1users/operations/get")).isEmpty());
    }

    @Test
    void testFindById_WhenDocumentIsCopied_ShouldFindCopyAtSameLocation() {
        ApiDocument original = ModelFixture.document();
        DocumentIndex originalIndex = DocumentIndex.build(original);
        DocumentIndex copyIndex = DocumentIndex.build(original.copy());

        for (DocumentEntry entry : originalIndex.entries()) {
            if (entry.value() instanceof IdentifiedElement element) {
                DocumentEntry copy = copyIndex.findById(element.getId()).orElseThrow();
                assertEquals(entry.path(), copy.path());
                assertEquals(entry.value().getClass(), copy.value().getClass());
            }
        }
    }

    @Test
    void testFind_WhenElementDoesNotExist_ShouldReturnEmptyResult() {
        DocumentIndex index = DocumentIndex.build(new ApiDocument(ElementId.random()));

        assertTrue(index.findById(ElementId.random()).isEmpty());
        assertTrue(index.findByPath(DocumentPath.parse("/missing")).isEmpty());
        assertThrows(UnsupportedOperationException.class, index.entries()::clear);
    }
}
