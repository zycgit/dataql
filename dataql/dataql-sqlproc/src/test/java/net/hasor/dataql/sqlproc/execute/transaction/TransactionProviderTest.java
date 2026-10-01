/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransactionProviderTest {
    private String              jdbcUrl;
    private TransactionProvider connectionProvider;

    @Before
    public void setupDatabase() throws SQLException {
        this.jdbcUrl = "jdbc:h2:mem:provider_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=2000";
        this.connectionProvider = new TransactionProvider((name, hints) -> this.rawConnection());
        try (Connection connection = this.rawConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tx_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, label VARCHAR(100))");
        }
    }

    private Connection rawConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
    }

    @After
    public void closeTransactionProvider() throws Exception {
        this.connectionProvider.close();
    }

    @Test
    public void requiredJoinsOuterTransactionAndReturnsCallbackResult() throws Throwable {
        Object expected = new Object();
        Object result = this.execute(Propagation.REQUIRED, () -> {
            this.insert("outer");
            Object nestedResult = this.execute(Propagation.REQUIRED, () -> {
                this.insert("inner");
                return expected;
            });
            assertEquals(0, this.count("outer"));
            assertEquals(0, this.count("inner"));
            return nestedResult;
        });

        assertSame(expected, result);
        assertEquals(1, this.count("outer"));
        assertEquals(1, this.count("inner"));
    }

    private Object execute(Propagation propagation, TransactionCallback callback) throws Throwable {
        return this.connectionProvider.execute(null, null, propagation, Isolation.DEFAULT, callback);
    }

    private void insert(String label) throws SQLException {
        try (Connection connection = this.connectionProvider.findConnection(null, null)) {
            this.insert(connection, label);
        }
    }

    private void insert(Connection connection, String label) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO tx_item (label) VALUES (?)")) {
            statement.setString(1, label);
            statement.executeUpdate();
        }
    }

    private int count(String label) throws SQLException {
        try (Connection connection = this.rawConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM tx_item WHERE label = ?")) {
            statement.setString(1, label);
            try (ResultSet results = statement.executeQuery()) {
                assertTrue(results.next());
                return results.getInt(1);
            }
        }
    }

    @Test
    public void requiredFailureRollsBackAndLeavesProviderReusable() throws Throwable {
        SQLException expected = new SQLException("callback failed");
        SQLException failure = assertThrows(SQLException.class, () -> this.execute(Propagation.REQUIRED, () -> {
            this.insert("outer");
            return this.execute(Propagation.REQUIRED, () -> {
                this.insert("inner");
                throw expected;
            });
        }));

        assertSame(expected, failure);
        assertEquals(0, this.count("outer"));
        assertEquals(0, this.count("inner"));
        assertNull(this.execute(Propagation.REQUIRED, () -> {
            this.insert("next");
            return null;
        }));
        assertEquals(1, this.count("next"));
    }

    @Test
    public void requiresNewCommitsIndependentlyAndRestoresOuterConnection() throws Throwable {
        assertThrows(SQLException.class, () -> this.execute(Propagation.REQUIRED, () -> {
            this.insert("outer");
            try (Connection outer = this.connectionProvider.findConnection(null, null)) {
                Connection physical = outer.unwrap(Connection.class);
                this.execute(Propagation.REQUIRES_NEW, () -> {
                    try (Connection inner = this.connectionProvider.findConnection(null, null)) {
                        assertNotSame(physical, inner.unwrap(Connection.class));
                    }
                    this.insert("inner");
                    return null;
                });
                try (Connection restored = this.connectionProvider.findConnection(null, null)) {
                    assertSame(physical, restored.unwrap(Connection.class));
                }
            }
            throw new SQLException("roll back outer");
        }));

        assertEquals(0, this.count("outer"));
        assertEquals(1, this.count("inner"));
    }

    @Test
    public void nestedRollbackKeepsOuterTransactionUsable() throws Throwable {
        this.execute(Propagation.REQUIRED, () -> {
            this.insert("outer");
            assertThrows(SQLException.class, () -> this.execute(Propagation.NESTED, () -> {
                this.insert("nested");
                throw new SQLException("roll back savepoint");
            }));
            this.insert("after-savepoint");
            return null;
        });

        assertEquals(1, this.count("outer"));
        assertEquals(0, this.count("nested"));
        assertEquals(1, this.count("after-savepoint"));
    }

    @Test
    public void nestedSuccessStillRollsBackWithOuterTransaction() throws Throwable {
        assertThrows(SQLException.class, () -> this.execute(Propagation.REQUIRED, () -> {
            this.execute(Propagation.NESTED, () -> {
                this.insert("nested");
                return null;
            });
            throw new SQLException("roll back outer");
        }));

        assertEquals(0, this.count("nested"));
    }

    @Test
    public void supportsJoinsAnExistingTransaction() throws Throwable {
        assertThrows(SQLException.class, () -> this.execute(Propagation.REQUIRED, () -> {
            this.execute(Propagation.SUPPORTS, () -> {
                this.insert("supports-inner");
                return null;
            });
            throw new SQLException("roll back outer");
        }));

        assertEquals(0, this.count("supports-inner"));
    }

    @Test
    public void supportsAutocommitsWithoutAnExistingTransaction() throws Throwable {
        assertThrows(SQLException.class, () -> this.execute(Propagation.SUPPORTS, () -> {
            this.insert("supports-alone");
            throw new SQLException("no transaction to roll back");
        }));

        assertEquals(1, this.count("supports-alone"));
    }

    @Test
    public void notSupportedSuspendsOuterAndAutocommitsInnerWork() throws Throwable {
        assertThrows(SQLException.class, () -> this.execute(Propagation.REQUIRED, () -> {
            this.insert("outer");
            assertThrows(SQLException.class, () -> this.execute(Propagation.NOT_SUPPORTED, () -> {
                this.insert("not-supported");
                throw new SQLException("no transaction to roll back");
            }));
            this.insert("restored-outer");
            throw new SQLException("roll back outer");
        }));

        assertEquals(0, this.count("outer"));
        assertEquals(0, this.count("restored-outer"));
        assertEquals(1, this.count("not-supported"));
    }

    @Test
    public void neverAutocommitsWithoutTransactionAndRejectsAnExistingOne() throws Throwable {
        this.execute(Propagation.NEVER, () -> {
            this.insert("never-alone");
            return null;
        });
        this.execute(Propagation.REQUIRED, () -> {
            SQLException failure = assertThrows(SQLException.class, () -> this.execute(Propagation.NEVER, () -> {
                fail("NEVER must reject the transaction before invoking the callback.");
                return null;
            }));
            assertTrue(failure.getMessage().contains("Existing transaction"));
            this.insert("outer-after-rejection");
            return null;
        });

        assertEquals(1, this.count("never-alone"));
        assertEquals(1, this.count("outer-after-rejection"));
    }

    @Test
    public void mandatoryRequiresAndJoinsAnExistingTransaction() throws Throwable {
        SQLException failure = assertThrows(SQLException.class, () -> this.execute(Propagation.MANDATORY, () -> {
            fail("MANDATORY must reject a missing transaction before invoking the callback.");
            return null;
        }));
        assertTrue(failure.getMessage().contains("No existing transaction"));

        this.execute(Propagation.REQUIRED, () -> {
            this.execute(Propagation.MANDATORY, () -> {
                this.insert("mandatory");
                return null;
            });
            assertEquals(0, this.count("mandatory"));
            return null;
        });
        assertEquals(1, this.count("mandatory"));
    }

    @Test
    public void connectionSettingsAreRestoredAfterTransaction() throws Throwable {
        try (Connection borrowed = this.connectionProvider.findConnection(null, null)) {
            int originalIsolation = borrowed.getTransactionIsolation();
            this.connectionProvider.execute(null, null, Propagation.REQUIRED, Isolation.SERIALIZABLE, () -> {
                assertFalse(borrowed.getAutoCommit());
                assertEquals(Connection.TRANSACTION_SERIALIZABLE, borrowed.getTransactionIsolation());
                this.insert("serializable");
                return null;
            });
            assertTrue(borrowed.getAutoCommit());
            assertEquals(originalIsolation, borrowed.getTransactionIsolation());
        }
        assertEquals(1, this.count("serializable"));
    }

    @Test
    public void closingAProxyDoesNotCloseTheTransactionConnection() throws Throwable {
        AtomicReference<Connection> physical = new AtomicReference<>();
        this.execute(Propagation.REQUIRED, () -> {
            try (Connection first = this.connectionProvider.findConnection(null, null); Connection second = this.connectionProvider.findConnection(null, null)) {
                physical.set(first.unwrap(Connection.class));
                assertSame(physical.get(), second.unwrap(Connection.class));
                first.close();
                first.close();
                assertTrue(first.isClosed());
                assertThrows(SQLException.class, first::getAutoCommit);
                assertFalse(physical.get().isClosed());
                assertFalse(second.getAutoCommit());
            }
            this.insert("after-proxy-close");
            return null;
        });

        assertTrue(physical.get().isClosed());
        assertEquals(1, this.count("after-proxy-close"));
    }

    @Test
    public void failedConnectionAcquisitionCanBeRetried() throws Throwable {
        AtomicInteger attempts = new AtomicInteger();
        this.connectionProvider = new TransactionProvider((name, hints) -> {
            if (attempts.getAndIncrement() == 0) {
                throw new SQLException("connection unavailable");
            }
            return this.rawConnection();
        });
        assertThrows(SQLException.class, () -> this.execute(Propagation.REQUIRED, () -> null));
        this.execute(Propagation.REQUIRED, () -> {
            this.insert("retry");
            return null;
        });

        assertEquals(2, attempts.get());
        assertEquals(1, this.count("retry"));
    }

    @Test
    public void datasourceNamesKeepIndependentTransactions() throws Throwable {
        assertThrows(SQLException.class, () -> this.execute(Propagation.REQUIRED, () -> {
            this.insert("primary");
            this.connectionProvider.execute("secondary", null, Propagation.REQUIRED, Isolation.DEFAULT, () -> {
                try (Connection connection = this.connectionProvider.findConnection("secondary", null)) {
                    this.insert(connection, "secondary");
                }
                return null;
            });
            throw new SQLException("roll back primary");
        }));

        assertEquals(0, this.count("primary"));
        assertEquals(1, this.count("secondary"));
    }

    @Test
    public void concurrentCallsKeepIndependentTransactions() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> committed = workers.submit(() -> {
                try {
                    this.concurrentTransaction("committed", ready, false);
                } catch (Throwable failure) {
                    throw new AssertionError(failure);
                } finally {
                    this.connectionProvider.close();
                }
                return null;
            });
            Future<?> rolledBack = workers.submit(() -> {
                try {
                    SQLException failure = assertThrows(SQLException.class, () -> this.concurrentTransaction("rolled-back", ready, true));
                    assertEquals("roll back worker", failure.getMessage());
                } finally {
                    this.connectionProvider.close();
                }
                return null;
            });
            committed.get(15, TimeUnit.SECONDS);
            rolledBack.get(15, TimeUnit.SECONDS);
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(1, this.count("committed"));
        assertEquals(0, this.count("rolled-back"));
    }

    private void concurrentTransaction(String label, CountDownLatch ready, boolean rollback) throws Throwable {
        this.execute(Propagation.REQUIRED, () -> {
            this.insert(label);
            ready.countDown();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            if (rollback) {
                throw new SQLException("roll back worker");
            }
            return null;
        });
    }

}
