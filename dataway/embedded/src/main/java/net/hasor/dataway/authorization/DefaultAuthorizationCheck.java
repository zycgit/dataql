/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;

/** Allows every operation until the host supplies its own authorization policy. */
public class DefaultAuthorizationCheck implements AuthorizationCheck {
    @Override
    public boolean check(UserIdentity identity, Operation operation) {
        return true;
    }
}
