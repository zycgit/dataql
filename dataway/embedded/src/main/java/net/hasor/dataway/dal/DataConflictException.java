/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;

/** Revision mismatch or duplicate identity/route. No part of the write batch was committed. */
public class DataConflictException extends DataAccessException {
    public DataConflictException(String message) {
        super(message, null);
    }

    public DataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
