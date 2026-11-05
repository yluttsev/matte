package io.github.yluttsev.matte.core.editing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import io.github.yluttsev.matte.core.editing.command.AddSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.AddSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.DeleteSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.EditCommand;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaComponent;
import io.github.yluttsev.matte.core.editing.command.RenameSchemaProperty;
import io.github.yluttsev.matte.core.editing.command.UpdateSchemaObject;
import io.github.yluttsev.matte.core.model.ApiDocument;
import io.github.yluttsev.matte.core.model.ElementId;
import io.github.yluttsev.matte.core.model.schema.SchemaComponent;
import io.github.yluttsev.matte.core.model.schema.SchemaObject;
import io.github.yluttsev.matte.core.model.schema.SchemaProperty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaCommandHandlerTest {

    @Test
    void testSupports_WhenCommandIsPermitted_ShouldMatchExactlyOneRegisteredHandler() {
        ElementId id = ElementId.random();
        SchemaObject schema = SchemaObject.builder(id).build();
        List<EditCommand> commands = List.of(
                new AddSchemaComponent(new SchemaComponent(ElementId.random(), "User", schema)),
                new AddSchemaProperty(id, new SchemaProperty(ElementId.random(), "name", schema)),
                new RenameSchemaComponent(id, "Person"),
                new RenameSchemaProperty(id, ElementId.random(), "title"),
                new DeleteSchemaProperty(id, ElementId.random()),
                new DeleteSchemaComponent(id),
                new UpdateSchemaObject(schema)
        );
        SchemaCommandHandler dispatcher = new SchemaCommandHandler(DefaultSchemaCommandHandlers.create());
        Set<Class<?>> commandTypes = commands.stream().map(Object::getClass).collect(Collectors.toSet());
        Set<Class<?>> permittedTypes = Arrays.stream(EditCommand.class.getPermittedSubclasses())
                .collect(Collectors.toSet());

        assertEquals(permittedTypes, commandTypes);
        for (EditCommand command : commands) {
            assertEquals(1, dispatcher.handlers().stream().filter(handler -> handler.supports(command)).count());
        }
        assertThrows(UnsupportedOperationException.class, () -> dispatcher.handlers().clear());
    }

    @Test
    void testApply_WhenNoHandlerMatches_ShouldRejectUnsupportedCommand() {
        SchemaCommandHandler dispatcher = new SchemaCommandHandler(List.of());
        EditCommand command = new DeleteSchemaComponent(ElementId.random());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dispatcher.apply(null, null, null, command));

        assertTrue(exception.getMessage().contains("Unsupported command"));
    }

    @Test
    void testApply_WhenMultipleHandlersMatch_ShouldRejectBeforeCallingEitherHandler() {
        RecordingHandler first = new RecordingHandler();
        RecordingHandler second = new RecordingHandler();
        SchemaCommandHandler dispatcher = new SchemaCommandHandler(List.of(first, second));
        EditCommand command = new DeleteSchemaComponent(ElementId.random());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dispatcher.apply(null, null, null, command));

        assertTrue(exception.getMessage().contains("Multiple handlers"));
        assertFalse(first.called);
        assertFalse(second.called);
    }

    @Test
    void testApply_WhenOneHandlerMatches_ShouldCallOnlyThatHandler() {
        RecordingHandler selected = new RecordingHandler();
        SchemaCommandHandler dispatcher = new SchemaCommandHandler(List.of(
                new AddSchemaComponentHandler(), selected, new AddSchemaPropertyHandler()
        ));

        EditResult result = dispatcher.apply(null, null, null,
                new DeleteSchemaComponent(ElementId.random()));

        assertInstanceOf(EditRejected.class, result);
        assertTrue(selected.called);
    }

    @Test
    void testApply_WhenHandlersAreInjectedIntoDocumentEditor_ShouldUseSuppliedSnapshot() {
        RecordingHandler selected = new RecordingHandler();
        List<EditCommandHandler<?>> handlers = new ArrayList<>(List.of(selected));
        DocumentEditor editor = new DocumentEditor(handlers);
        handlers.clear();
        ApiDocument original = ApiDocument.builder(ElementId.random()).build();

        EditResult result = editor.apply(original, new DeleteSchemaComponent(ElementId.random()));

        assertInstanceOf(EditRejected.class, result);
        assertTrue(selected.called);
    }

    private static final class RecordingHandler implements EditCommandHandler<DeleteSchemaComponent> {

        private boolean called;

        @Override
        public Class<DeleteSchemaComponent> commandType() {
            return DeleteSchemaComponent.class;
        }

        @Override
        public EditResult handle(EditContext context, DeleteSchemaComponent command) {
            called = true;
            return EditResults.rejected(EditCode.TARGET_NOT_FOUND, "recorded");
        }
    }
}
