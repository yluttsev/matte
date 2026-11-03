package io.github.yluttsev.matte.core.construction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ModelAssertions {

    private ModelAssertions() {
    }

    public static Set<CopyableModel<?>> modelNodes(CopyableModel<?> root) {
        Set<CopyableModel<?>> nodes = Collections.newSetFromMap(new IdentityHashMap<>());
        collectNodes(root, nodes);
        return nodes;
    }

    public static void assertReadOnlyCollections(CopyableModel<?> root) {
        for (CopyableModel<?> node : modelNodes(root)) {
            forEachGetter(node, getter -> assertReadOnly(invoke(getter, node), getter.toString()));
        }
    }

    private static void assertReadOnly(Object value, String path) {
        if (value instanceof Map<?, ?> entries) {
            assertThrows(UnsupportedOperationException.class, entries::clear, path);
            entries.values().forEach(entry -> assertReadOnly(entry, path));
        } else if (value instanceof Collection<?> entries) {
            assertThrows(UnsupportedOperationException.class, entries::clear, path);
            entries.forEach(entry -> assertReadOnly(entry, path));
        }
    }

    public static void assertIndependentCopy(Object source, Object copy) {
        assertIndependentCopy(source, copy, "$");
    }

    private static void assertIndependentCopy(Object source, Object copy, String path) {
        if (source == null) {
            assertNull(copy, path);
        } else if (source instanceof CopyableModel<?>) {
            assertNotNull(copy, path);
            assertNotSame(source, copy, path);
            assertEquals(source.getClass(), copy.getClass(), path);
            forEachGetter(source, getter -> assertIndependentCopy(
                    invoke(getter, source), invoke(getter, copy), path + "." + getter.getName()));
        } else if (source instanceof Map<?, ?> sourceMap) {
            Map<?, ?> copyMap = (Map<?, ?>) copy;
            assertEquals(new ArrayList<>(sourceMap.keySet()), new ArrayList<>(copyMap.keySet()), path);
            sourceMap.forEach((key, value) -> assertIndependentCopy(value, copyMap.get(key), path + "[" + key + "]"));
        } else if (source instanceof Collection<?> sourceItems) {
            ArrayList<?> sourceList = new ArrayList<>(sourceItems);
            ArrayList<?> copyList = new ArrayList<>((Collection<?>) copy);
            assertEquals(sourceList.size(), copyList.size(), path);
            for (int index = 0; index < sourceList.size(); index++) {
                assertIndependentCopy(sourceList.get(index), copyList.get(index), path + "[" + index + "]");
            }
        } else {
            assertEquals(source, copy, path);
        }
    }

    private static void collectNodes(Object value, Set<CopyableModel<?>> nodes) {
        if (value instanceof CopyableModel<?> model) {
            if (nodes.add(model)) {
                forEachGetter(model, getter -> collectNodes(invoke(getter, model), nodes));
            }
        } else if (value instanceof Map<?, ?> entries) {
            entries.values().forEach(entry -> collectNodes(entry, nodes));
        } else if (value instanceof Iterable<?> entries) {
            entries.forEach(entry -> collectNodes(entry, nodes));
        }
    }

    private static void forEachGetter(Object model, Consumer<Method> action) {
        if (model.getClass().isRecord()) {
            for (RecordComponent component : model.getClass().getRecordComponents()) {
                action.accept(component.getAccessor());
            }
            return;
        }
        for (Method method : model.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers())
                    && method.getDeclaringClass() != Object.class
                    && isGetter(method.getName())) {
                action.accept(method);
            }
        }
    }

    private static boolean isGetter(String name) {
        return name.length() > 3 && (name.startsWith("get") || name.startsWith("has"))
                && Character.isUpperCase(name.charAt(3));
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot read " + method, exception);
        }
    }
}
