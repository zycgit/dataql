/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;

/** Checks whether the host-resolved identity may perform the entry's operation. */
@FunctionalInterface
public interface AuthorizationCheck {
    boolean check(UserIdentity identity, Operation operation);
}