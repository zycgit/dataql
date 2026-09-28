/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;

/** Actions grouped by published API invocation and administration. */
public enum Operation {
    // Published APIs
    INVOKE,

    // API specifications
    DOCUMENT,

    // Administration
    LIST,
    READ,
    HISTORY,
    SAVE,
    PUBLISH,
    DISABLE,
    DELETE,
    DEBUG
}
