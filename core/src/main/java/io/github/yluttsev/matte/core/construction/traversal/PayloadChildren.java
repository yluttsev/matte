package io.github.yluttsev.matte.core.construction.traversal;

import io.github.yluttsev.matte.core.model.media.Encoding;
import io.github.yluttsev.matte.core.model.media.Example;
import io.github.yluttsev.matte.core.model.media.MediaType;
import io.github.yluttsev.matte.core.model.media.RequestBody;
import io.github.yluttsev.matte.core.model.parameter.Header;
import io.github.yluttsev.matte.core.model.parameter.Parameter;
import io.github.yluttsev.matte.core.model.response.ApiResponse;
import java.util.function.BiConsumer;

final class PayloadChildren implements ModelChildren {

    @Override
    public boolean visitChildren(Object node, BiConsumer<Object, String> visitor) {
        if (node instanceof Parameter parameter) {
            visitor.accept(parameter.getSchema(), "schema");
            visitor.accept(parameter.getExamples(), "examples");
            visitor.accept(parameter.getContent(), "content");
        } else if (node instanceof Header header) {
            visitor.accept(header.getSchema(), "schema");
            visitor.accept(header.getExamples(), "examples");
            visitor.accept(header.getContent(), "content");
        } else if (node instanceof RequestBody body) {
            visitor.accept(body.getContent(), "content");
        } else if (node instanceof ApiResponse response) {
            visitor.accept(response.getHeaders(), "headers");
            visitor.accept(response.getContent(), "content");
        } else if (node instanceof MediaType mediaType) {
            visitor.accept(mediaType.getSchema(), "schema");
            visitor.accept(mediaType.getExamples(), "examples");
            visitor.accept(mediaType.getEncoding(), "encoding");
        } else if (node instanceof Encoding encoding) {
            visitor.accept(encoding.getHeaders(), "headers");
        } else {
            return node instanceof Example;
        }
        return true;
    }
}
