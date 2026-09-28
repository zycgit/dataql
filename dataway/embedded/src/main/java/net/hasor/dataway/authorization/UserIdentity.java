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
import java.util.Set;

/** Request identity supplied by the host. Dataway does not own authentication or sessions. */
public final class UserIdentity {
    private final String         identityId;
    private final boolean        authenticated;
    private final Map<String, ?> attributes;
    private final Set<Operation> operations;

    private UserIdentity(String identityId, boolean authenticated, Map<String, ?> attributes, Set<Operation> operations) {
        if (authenticated && (identityId == null || identityId.isBlank())) {
            throw new IllegalArgumentException("An authenticated identity requires an id");
        }

        this.identityId = identityId;
        this.authenticated = authenticated;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.operations = Set.copyOf(operations);
    }

    public String identityId() {
        return this.identityId;
    }

    public boolean authenticated() {
        return this.authenticated;
    }

    public Map<String, ?> attributes() {
        return this.attributes;
    }

    public Set<Operation> operations() {
        return this.operations;
    }

    public boolean checkOperation(Operation operation) {
        return operation != null && this.operations.contains(operation);
    }

    //

    /** No authenticated user and no operation permissions. */
    public static UserIdentity anonymous(Map<String, ?> attributes) {
        return new UserIdentity(null, false, attributes, Set.of());
    }

    /** Allows published API calls; authentication alone grants no console access. */
    public static UserIdentity authenticated(String identityId, Map<String, ?> attributes) {
        return new UserIdentity(identityId, true, attributes, Set.of(Operation.INVOKE, Operation.DOCUMENT));
    }

    /** Allows published API calls and read-only console operations. */
    public static UserIdentity consoleReadOnly(String identityId, Map<String, ?> attributes) {
        return new UserIdentity(identityId, true, attributes, Set.of(Operation.INVOKE, Operation.DOCUMENT, Operation.LIST, Operation.READ, Operation.HISTORY));
    }

    /** Allows every API and administration operation. */
    public static UserIdentity consoleAdmin(String identityId, Map<String, ?> attributes) {
        return new UserIdentity(identityId, true, attributes, Set.of(Operation.values()));
    }
}
