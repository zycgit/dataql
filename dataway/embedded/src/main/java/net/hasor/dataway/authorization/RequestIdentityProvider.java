/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import java.util.Map;
import net.hasor.dataway.model.WebRequest;

/** Reads the identity placed in a request attribute by the host's authentication logic. */
public class RequestIdentityProvider implements IdentityProvider {
    private final String attributeName;

    public RequestIdentityProvider(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public UserIdentity resolve(WebRequest request) {
        Object identity = request.getAttribute(this.attributeName);
        return identity instanceof UserIdentity user ? user : UserIdentity.anonymous(Map.of());
    }
}
