/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.real.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionProvider;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransactionDataQLIntegrationTest {
    private String                   jdbcUrl;
    private AtomicReference<String>  sourceName;
    private AtomicReference<Object>  tenantHint;
    private AtomicReference<Integer> isolationHint;

    @Before
    public void setupDatabase() throws Exception {
        this.jdbcUrl = "jdbc:h2:mem:real_tx_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        this.sourceName = new AtomicReference<>();
        this.tenantHint = new AtomicReference<>();
        this.isolationHint = new AtomicReference<>();
        try (Connection conn = rawConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE tx_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, label VARCHAR(100))");
        }
    }

    @Test
    public void requiredTransactionCommitsSqlFragments() throws Exception {
        Query query = dataQL().createQuery("""
                hint FRAGMENT_SQL_DATA_SOURCE = "txDs"
                hint tenant = "north"
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                var countItem = @@selectSql(label)<%
                    SELECT COUNT(*) AS cnt FROM tx_item WHERE label = :label
                %>;
                return tran.required(() -> {
                    run addItem("commit");
                    return countItem("commit");
                });
                """);

        Object result = query.execute().getData().unwrap();

        assertNumber(1, result);
        assertEquals(1, count("commit"));
        assertEquals("txDs", this.sourceName.get());
        assertEquals("north", this.tenantHint.get());
    }

    @Test
    public void transactionCallbacksReceiveObjectHints() throws Exception {
        Object binding = new Object();
        QueryBuilder builder = this.dataQL();
        builder.addShareVar("inspect", () -> (Udf) (hints, params) -> {
            assertSame(binding, hints.getHint("tenant"));
            return true;
        });
        Query query = builder.createQuery("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                return tran.required(inspect);
                """);
        query.setHint("tenant", binding);

        assertEquals(true, query.execute().getData().unwrap());
        assertSame(binding, this.tenantHint.get());
    }

    @Test
    public void transactionIsolationComesFromDataQLHints() throws Exception {
        Query query = dataQL().createQuery("""
                hint FRAGMENT_SQL_TRANSACTION_ISOLATION = "SERIALIZABLE"
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                return tran.required(() -> {
                    run addItem("serializable");
                    return true;
                });
                """);

        query.execute();

        assertEquals(Integer.valueOf(Connection.TRANSACTION_SERIALIZABLE), this.isolationHint.get());
        assertEquals(1, count("serializable"));
    }

    @Test
    public void requiredTransactionRollsBackWhenDataQLFails() throws Exception {
        Query query = dataQL().createQuery("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                var failSql = @@insertSql()<%
                    INSERT INTO missing_table (label) VALUES ('rollback')
                %>;
                return tran.required(() -> {
                    run addItem("rollback");
                    run failSql();
                    return true;
                });
                """);

        try {
            query.execute();
            fail("transaction should roll back when SQL execution fails.");
        } catch (Throwable e) {
            assertEquals(0, count("rollback"));
        }
    }

    @Test
    public void requiresNewCommitsWhenOuterTransactionFails() throws Exception {
        executeExpectFailure("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                var failSql = @@insertSql()<%
                    INSERT INTO missing_table (label) VALUES ('rollback')
                %>;
                return tran.required(() -> {
                    var ignored = tran.requiresNew(() -> {
                        run addItem("requires-new");
                        return true;
                    });
                    run addItem("requires-new-outer");
                    run failSql();
                    return ignored;
                });
                """);

        assertEquals(1, count("requires-new"));
        assertEquals(0, count("requires-new-outer"));
    }

    @Test
    public void nestedTransactionRunsInsideOuterTransaction() throws Exception {
        Query query = dataQL().createQuery("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                return tran.required(() -> {
                    run addItem("nested-outer");
                    return tran.nested(() -> {
                        run addItem("nested-inner");
                        return true;
                    });
                });
                """);

        query.execute();

        assertEquals(1, count("nested-outer"));
        assertEquals(1, count("nested-inner"));
    }

    @Test
    public void supportsJoinsOuterTransactionWhenItExists() throws Exception {
        executeExpectFailure("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                var failSql = @@insertSql()<%
                    INSERT INTO missing_table (label) VALUES ('rollback')
                %>;
                return tran.required(() -> {
                    var ignored = tran.supports(() -> {
                        run addItem("supports-inner");
                        return true;
                    });
                    run failSql();
                    return ignored;
                });
                """);

        assertEquals(0, count("supports-inner"));
    }

    @Test
    public void supportsAutocommitsWhenNoTransactionExists() throws Exception {
        Query query = dataQL().createQuery("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                return tran.supports(() -> {
                    run addItem("supports-alone");
                    return true;
                });
                """);

        query.execute();

        assertEquals(1, count("supports-alone"));
    }

    @Test
    public void notSupportedAutocommitsWhenOuterTransactionFails() throws Exception {
        executeExpectFailure("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                var failSql = @@insertSql()<%
                    INSERT INTO missing_table (label) VALUES ('rollback')
                %>;
                return tran.required(() -> {
                    var ignored = tran.notSupported(() -> {
                        run addItem("not-supported");
                        return true;
                    });
                    run addItem("not-supported-outer");
                    run failSql();
                    return ignored;
                });
                """);

        assertEquals(1, count("not-supported"));
        assertEquals(0, count("not-supported-outer"));
    }

    @Test
    public void neverAutocommitsWithoutOuterTransaction() throws Exception {
        Query query = dataQL().createQuery("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                return tran.never(() -> {
                    run addItem("never-alone");
                    return true;
                });
                """);

        query.execute();

        assertEquals(1, count("never-alone"));
    }

    @Test
    public void neverFailsInsideOuterTransaction() throws Exception {
        executeExpectFailure("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                return tran.required(() -> {
                    run addItem("never-outer");
                    return tran.never(() -> {
                        run addItem("never-inner");
                        return true;
                    });
                });
                """);

        assertEquals(0, count("never-outer"));
        assertEquals(0, count("never-inner"));
    }

    @Test
    public void mandatoryFailsWithoutOuterTransaction() throws Exception {
        executeExpectFailure("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                return tran.mandatory(() -> {
                    run addItem("mandatory-alone");
                    return true;
                });
                """);

        assertEquals(0, count("mandatory-alone"));
    }

    @Test
    public void mandatoryJoinsOuterTransactionWhenItExists() throws Exception {
        Query query = dataQL().createQuery("""
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var addItem = @@insertSql(label)<%
                    INSERT INTO tx_item (label) VALUES (:label)
                %>;
                return tran.required(() -> {
                    return tran.mandatory(() -> {
                        run addItem("mandatory-inner");
                        return true;
                    });
                });
                """);

        query.execute();

        assertEquals(1, count("mandatory-inner"));
    }

    @Test
    public void transactionFunctionRequiresTransactionConnectionProvider() throws Exception {
        HostConfiguration configuration = new HostConfiguration();
        configuration.addAttachment(ConnectionProvider.class, (sourceName, hints) -> wrapConnection(rawConnection()));

        try {
            new QueryManager(configuration.getHostContext()).newBuilder().createQuery("""
                    import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                    return tran.required(() -> {
                        return true;
                    });
                    """).execute();
            fail("Transaction function should require TransactionConnectionProvider.");
        } catch (Throwable e) {
            assertTrue(hasMessage(e, "ConnectionProvider must implement TransactionalProvider when using transaction functions."));
        }
    }

    private QueryBuilder dataQL() {
        HostConfiguration configuration = new HostConfiguration();
        configuration.addAttachment(ConnectionProvider.class, new TransactionProvider((sourceName, hints) -> {
            this.sourceName.set(sourceName);
            this.tenantHint.set(hints == null ? null : hints.getHint("tenant"));
            return wrapConnection(rawConnection());
        }));
        return new QueryManager(configuration.getHostContext()).newBuilder();
    }

    private void executeExpectFailure(String dataql) {
        try {
            dataQL().createQuery(dataql).execute();
            fail("DataQL execution should fail.");
        } catch (Throwable e) {
            // Expected by the scenario.
        }
    }

    private int count(String label) throws SQLException {
        try (Connection conn = rawConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tx_item WHERE label = '" + label + "'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Connection rawConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
    }

    private Connection wrapConnection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class }, (proxy, method, args) -> {
            if (isSetSerializable(method, args)) {
                this.isolationHint.set(Connection.TRANSACTION_SERIALIZABLE);
            }
            try {
                return method.invoke(connection, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        });
    }

    private static boolean isSetSerializable(Method method, Object[] args) {
        return "setTransactionIsolation".equals(method.getName()) && args != null && args.length == 1 && Integer.valueOf(Connection.TRANSACTION_SERIALIZABLE).equals(args[0]);
    }

    private static boolean hasMessage(Throwable e, String message) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (message.equals(current.getMessage())) {
                return true;
            }
        }
        return false;
    }

    private void assertNumber(long expected, Object value) {
        assertNotNull(value);
        assertEquals(expected, ((Number) value).longValue());
    }
}
