/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataAccessExceptionTest {
    @Test
    void storageFailurePreservesTheProviderCause() {
        Exception cause = new Exception("Connection closed");
        DataAccessException failure = new DataAccessException("Read failed", cause);

        assertEquals("Read failed", failure.getMessage());
        assertSame(cause, failure.getCause());
    }

    @Test
    void conflictsCanBeCaughtAsStorageFailuresWithOrWithoutACause() {
        DataAccessException conflict = new DataConflictException("Duplicate route");
        assertEquals("Duplicate route", conflict.getMessage());
        assertNull(conflict.getCause());

        Exception cause = new Exception("Compare-and-set failed");
        DataAccessException wrapped = new DataConflictException("Revision changed", cause);
        assertEquals("Revision changed", wrapped.getMessage());
        assertSame(cause, wrapped.getCause());
    }
}
