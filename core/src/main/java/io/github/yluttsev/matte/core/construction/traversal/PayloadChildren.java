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
    public boolean visitChildren(Object node, String path, BiConsumer<Object, String> visitor) {
        if (node instanceof Parameter parameter) {
            visitor.accept(parameter.getSchema(), path + ".schema");
            visitor.accept(parameter.getExamples(), path + ".examples");
            visitor.accept(parameter.getContent(), path + ".content");
        } else if (node instanceof Header header) {
            visitor.accept(header.getSchema(), path + ".schema");
            visitor.accept(header.getExamples(), path + ".examples");
            visitor.accept(header.getContent(), path + ".content");
        } else if (node instanceof RequestBody body) {
            visitor.accept(body.getContent(), path + ".content");
        } else if (node instanceof ApiResponse response) {
            visitor.accept(response.getHeaders(), path + ".headers");
            visitor.accept(response.getContent(), path + ".content");
        } else if (node instanceof MediaType mediaType) {
            visitor.accept(mediaType.getSchema(), path + ".schema");
            visitor.accept(mediaType.getExamples(), path + ".examples");
            visitor.accept(mediaType.getEncoding(), path + ".encoding");
        } else if (node instanceof Encoding encoding) {
            visitor.accept(encoding.getHeaders(), path + ".headers");
        } else {
            return node instanceof Example;
        }
        return true;
    }
}
