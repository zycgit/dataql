/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import net.hasor.dataway.dal.EntityType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionTest {
    @Test
    void nestedCallbacksShareConnectionAndCommitOnlyAtOuterBoundary() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            LocalJdbcExecutor executor = new LocalJdbcExecutor(fixture.source);
            Connection[] observed = new Connection[1];
            assertEquals("result", executor.execute(connection -> {
                observed[0] = connection;
                assertFalse(connection.getAutoCommit());
                executor.execute(nested -> {
                    assertSame(connection, nested);
                    nested.createStatement().executeUpdate("INSERT INTO interface_info (api_id, api_method, api_path, api_status, api_comment, api_type, api_script, api_schema, api_sample, api_option, api_create_time, api_gmt_time) VALUES ('tx','GET','/tx','0','','DataQL','return 1;','{}','{}','{}','1','1')");
                    return null;
                });
                return "result";
            }));
            assertTrue(observed[0].isClosed());
            assertTrue(fixture.access.getObject(EntityType.INFO, "tx").isPresent());
        }
    }

    @Test
    void caughtNestedFailureStillMarksTheOuterTransactionRollbackOnly() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            LocalJdbcExecutor executor = new LocalJdbcExecutor(fixture.source);
            SQLException original = new SQLException("nested failure");
            SQLException outer = assertThrows(SQLException.class, () -> executor.execute(connection -> {
                assertSame(original, assertThrows(SQLException.class, () -> executor.execute(nested -> {
                    throw original;
                })));
                return "must not commit";
            }));
            assertTrue(outer.getMessage().contains("rollback-only"));
            assertEquals(3, executor.execute(connection -> 3).intValue());
        }
    }

    @Test
    void rollbackFailureIsSuppressedAndConnectionsAlwaysClose() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        SQLException rollback = new SQLException("rollback failed");
        doThrow(rollback).when(connection).rollback();
        AssertionError original = new AssertionError("execution failed");
        LocalJdbcExecutor executor = new LocalJdbcExecutor(source);
        assertSame(original, assertThrows(AssertionError.class, () -> executor.execute(current -> {
            throw original;
        })));
        assertArrayEquals(new Throwable[] { rollback }, original.getSuppressed());
        verify(connection).close();
        verify(connection, never()).commit();
    }

    @Test
    void standaloneExecutorRejectsAnAlreadyActiveHostTransaction() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);
        assertThrows(SQLException.class, () -> new LocalJdbcExecutor(source).execute(current -> fail("callback must not run")));
        verify(connection, never()).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }
}
