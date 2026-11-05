package io.github.yluttsev.matte.core.reference;

import java.net.URI;

/** URI classification shared by reference resolution and editing. */
public final class ReferenceUris {

    private ReferenceUris() {
    }

    public static boolean isExternal(URI uri) {
        return uri.getScheme() != null
                || uri.getRawAuthority() != null
                || uri.getRawQuery() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty());
    }
}
