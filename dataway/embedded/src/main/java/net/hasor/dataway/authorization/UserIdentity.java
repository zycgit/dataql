/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Request identity supplied by the host. Dataway does not own authentication or sessions. */
public record UserIdentity(String id, boolean authenticated, Map<String, ?> attributes) {
    private static final UserIdentity ANONYMOUS = new UserIdentity(null, false, Map.of());

    public UserIdentity(String id, boolean authenticated, Map<String, ?> attributes) {
        if (authenticated && (id == null || id.isBlank())) {
            throw new IllegalArgumentException("An authenticated identity requires an id");
        }

        this.id = id;
        this.authenticated = authenticated;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public static UserIdentity anonymous() {
        return ANONYMOUS;
    }

    public static UserIdentity authenticated(String id) {
        return new UserIdentity(id, true, Map.of());
    }
}
