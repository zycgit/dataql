/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.dal.DataAccessException;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import org.junit.jupiter.api.Test;
import static net.hasor.dataway.dal.FieldDef.API_ID;
import static net.hasor.dataway.dal.FieldDef.GMT_TIME;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StorageFailureTest {
    @Test
    void emptyBatchesAndInvalidMutationsNeverAcquireAConnection() throws Exception {
        JdbcExecutor executor = mock(JdbcExecutor.class);
        var access = new JdbcDataAccessLayer(executor);
        access.write(List.of());
        assertThrows(IllegalArgumentException.class, () -> access.createObject(EntityType.INFO, "bad", Map.of(API_ID, "not supported")));
        assertThrows(IllegalArgumentException.class, () -> access.listObjects(EntityType.RELEASE, Map.of(GMT_TIME, "not supported")));
        verifyNoInteractions(executor);
    }

    @Test
    void connectionAndNonUniqueSqlErrorsPreserveTheUnderlyingCause() throws Exception {
        JdbcExecutor executor = mock(JdbcExecutor.class);
        SQLException cause = new SQLException("connection unavailable", "08001");
        when(executor.execute(any())).thenThrow(cause);
        var access = new JdbcDataAccessLayer(executor);
        assertSame(cause, assertThrows(DataAccessException.class, () -> access.listObjects(EntityType.INFO, Map.of())).getCause());
        assertSame(cause, assertThrows(DataAccessException.class, () -> access.createObject(EntityType.INFO, "one", JdbcFixture.info("GET", "/one"))).getCause());
    }

    @Test
    void duplicateKeysAreRecognizedThroughJdbcNextExceptionsAndCauses() throws Exception {
        for (boolean next : new boolean[] { true, false }) {
            SQLException duplicate = next ? new SQLException("unique", "23505") : new SQLException("unique", "HY000", 1062);
            SQLException wrapper = new SQLException("driver wrapper");
            if (next) {
                wrapper.setNextException(duplicate);
            } else {
                wrapper.initCause(duplicate);
            }
            JdbcExecutor executor = mock(JdbcExecutor.class);
            when(executor.execute(any())).thenThrow(wrapper);
            var access = new JdbcDataAccessLayer(executor);
            assertSame(wrapper, assertThrows(DataConflictException.class, () -> access.createObject(EntityType.INFO, "one", JdbcFixture.info("GET", "/one"))).getCause());
        }
    }
}
