package io.github.yluttsev.matte.openapi;

import java.util.Objects;
import java.util.Optional;
import io.github.yluttsev.matte.core.editing.DocumentEditor;
import io.github.yluttsev.matte.core.editing.EditResult;
import io.github.yluttsev.matte.core.editing.EditSucceeded;
import io.github.yluttsev.matte.core.editing.change.Change;
import io.github.yluttsev.matte.core.editing.change.SchemaComponentAdded;
import io.github.yluttsev.matte.core.editing.change.SchemaPropertyAdded;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.model.schema.Schema;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import io.github.yluttsev.matte.core.navigation.DocumentIndex;
import io.github.yluttsev.matte.core.navigation.DocumentPath;
import io.github.yluttsev.matte.openapi.patch.OpenApiAdditionPatcher;
import io.github.yluttsev.matte.openapi.patch.OpenApiSourcePatcher;
import io.github.yluttsev.matte.openapi.patch.SchemaAdditionSerializer;

public final class OpenApiSchemaEditor {

    public RenameResult rename(ImportResult imported, RenameSchemaComponent command) {
        OpenApiEditResult result = apply(imported, command);
        return new RenameResult(result.edit(), result.source());
    }

    public OpenApiEditResult addProperty(ImportResult imported, AddSchemaProperty command) {
        return apply(imported, command);
    }

    public OpenApiEditResult addComponent(ImportResult imported, AddSchemaComponent command) {
        return apply(imported, command);
    }

    private OpenApiEditResult apply(ImportResult imported, EditCommand command) {
        Objects.requireNonNull(imported, "imported");
        Objects.requireNonNull(command, "command");
        if (imported.status() != ImportStatus.IMPORTED) {
            throw new IllegalArgumentException("Only imported OpenAPI documents can be edited");
        }
        EditResult edit = new DocumentEditor().apply(imported.document().orElseThrow(), command);
        if (!(edit instanceof EditSucceeded succeeded)) {
            return new OpenApiEditResult(edit, Optional.empty());
        }
        SourceFormat format = imported.format().orElseThrow();
        SourceDocument source = command instanceof RenameSchemaComponent
                ? new OpenApiSourcePatcher().apply(imported.source(), format, succeeded.changeSet())
                : new OpenApiAdditionPatcher().apply(imported, succeeded.changeSet());
        ImportResult checked = new OpenApiImporter().importDocument(source.content(), ImportOptions.withFormat(format));
        if (checked.status() != ImportStatus.IMPORTED || !checked.version().equals(imported.version())) {
            throw new IllegalArgumentException("Patched OpenAPI document could not be imported");
        }
        if (!(command instanceof RenameSchemaComponent)) {
            verifyAddition(imported, checked, succeeded.changeSet().changes().getFirst());
        }
        return new OpenApiEditResult(edit, Optional.of(source));
    }

    private void verifyAddition(ImportResult before, ImportResult after, Change change) {
        DocumentPath path;
        Schema expected;
        if (change instanceof SchemaComponentAdded component) {
            path = DocumentPath.schemaComponent(component.name());
            expected = component.schema();
        } else if (change instanceof SchemaPropertyAdded property) {
            DocumentPath schema = DocumentIndex.build(before.document().orElseThrow())
                    .findById(property.schemaId()).orElseThrow().path();
            path = schema.child("properties").child(property.name());
            expected = property.schema();
        } else {
            throw new IllegalArgumentException("Unsupported schema addition");
        }
        Object added = DocumentIndex.build(after.document().orElseThrow())
                .findByPath(path).orElseThrow(() -> new IllegalArgumentException("Added schema was not imported"))
                .value();
        Schema actual = added instanceof SchemaComponent component ? component.getSchema()
                : added instanceof SchemaProperty property ? property.getSchema()
                        : added instanceof Schema schema ? schema : null;
        OpenApiVersion version = OpenApiVersion.supported(before.version().orElseThrow()).orElseThrow();
        SchemaAdditionSerializer serializer = new SchemaAdditionSerializer();
        if (actual == null || !serializer.serialize(expected, version).equals(serializer.serialize(actual, version))) {
            throw new IllegalArgumentException("Added schema was not preserved by the OpenAPI parser");
        }
    }

    public record RenameResult(EditResult edit, Optional<SourceDocument> source) {

        public RenameResult {
            Objects.requireNonNull(edit, "edit");
            Objects.requireNonNull(source, "source");
        }
    }
}
