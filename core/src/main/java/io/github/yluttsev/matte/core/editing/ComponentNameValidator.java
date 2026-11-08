package io.github.yluttsev.matte.core.editing;

import java.util.regex.Pattern;

final class ComponentNameValidator {

    static final String INVALID_NAME_MESSAGE = "Component name must match ^[a-zA-Z0-9.\\-_]+$";

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9.\\-_]+$");

    private ComponentNameValidator() {
    }

    static boolean isValid(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }
}
