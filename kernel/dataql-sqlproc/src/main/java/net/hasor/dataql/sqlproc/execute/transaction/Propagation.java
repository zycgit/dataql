/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;

/** Transaction propagation behavior. */
public enum Propagation {
    REQUIRED,
    REQUIRES_NEW,
    NESTED,
    SUPPORTS,
    NOT_SUPPORTED,
    NEVER,
    MANDATORY,
}
