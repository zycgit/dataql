/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.api;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionCallback;
import net.hasor.dataway.solon.testcase.ExampleServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranUtils;
import static org.junit.jupiter.api.Assertions.*;

class TransactionIntegrationTest {
    private ExampleServer application;
    private DataSource    source;
    private QueryBuilder  builder;

    @BeforeEach
    void start() throws Throwable {
        this.application = new ExampleServer();
        this.source = this.application.source("ds1");
        ConnectionProvider provider = this.application.context().getBean(ConnectionProvider.class);
        HostConfiguration host = new HostConfiguration();
        host.addAttachment(ConnectionProvider.class, provider);
        this.builder = new QueryManager(host).newBuilder();
        this.builder.addShareVar("hostUpdate", () -> (Udf) (hints, params) -> {
            this.applicationUpdate();
            return true;
        });
    }

    @AfterEach
    void stop() throws Exception {
        if (this.application != null) {
            this.application.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "plain", "required" })
    void applicationTransactionIncludesSqlAndScriptFunctions(String mode) throws Throwable {
        String work = "run hostUpdate(); run change(1); return true;";
        String script = mode.equals("plain") ? work : "return tran.required(() -> { " + work + " });";
        this.applicationTransaction(() -> this.query(script));
        this.assertBalances(101, 201);

        assertThrows(IllegalStateException.class, () -> this.applicationTransaction(() -> {
            this.query(script);
            throw new IllegalStateException("Application failure");
        }));
        this.assertBalances(101, 201);
    }

    @Test
    void scriptTransactionIncludesApplicationDatabaseCalls() throws Throwable {
        this.query("return tran.required(() -> { run hostUpdate(); return change(1); });");
        this.assertBalances(101, 201);
        assertThrows(Exception.class, () -> this.query("return tran.required(() -> { run hostUpdate(); run change(1); return failSql(); });"));
        this.assertBalances(101, 201);
        this.query("return tran.required(() -> { run hostUpdate(); return change(1); });");
        this.assertBalances(102, 202);
    }

    @Test
    void requiresNewCommitsIndependentlyAndRestoresApplicationTransaction() throws Throwable {
        assertThrows(IllegalStateException.class, () -> this.applicationTransaction(() -> {
            this.applicationUpdate();
            this.query("return tran.requiresNew(() -> { return change(1); });");
            this.applicationUpdate();
            throw new IllegalStateException("Only the application transaction rolls back");
        }));
        this.assertBalances(101, 200);
    }

    @Test
    void mandatoryNeverAndSupportsUseTheHostTransactionState() throws Throwable {
        assertThrows(Exception.class, () -> this.query("return tran.mandatory(() -> { return change(1); });"));
        assertThrows(Exception.class, () -> this.applicationTransaction(() -> this.query("return tran.never(() -> { return change(1); });")));
        this.applicationTransaction(() -> this.query("return tran.mandatory(() -> { return change(1); });"));
        this.assertBalances(101, 200);
        assertThrows(IllegalStateException.class, () -> this.applicationTransaction(() -> {
            this.query("return tran.supports(() -> { return change(1); });");
            throw new IllegalStateException("Supports joins the host transaction");
        }));
        this.assertBalances(101, 200);
        this.query("return tran.never(() -> { return change(1); });");
        this.assertBalances(102, 200);
    }

    @Test
    void notSupportedSuspendsAndRestoresTheApplicationTransaction() throws Throwable {
        assertThrows(IllegalStateException.class, () -> this.applicationTransaction(() -> {
            this.applicationUpdate();
            this.query("return tran.notSupported(() -> { return change(1); });");
            this.applicationUpdate();
            throw new IllegalStateException("Application failure");
        }));
        this.assertBalances(101, 200);
    }

    @Test
    void nestedSuccessStillRollsBackWithTheApplicationTransaction() throws Throwable {
        assertThrows(IllegalStateException.class, () -> this.applicationTransaction(() -> {
            this.applicationUpdate();
            this.query("return tran.nested(() -> { return change(1); });");
            throw new IllegalStateException("Application failure");
        }));
        this.assertBalances(100, 200);
    }

    private Object query(String body) throws Exception {
        String script = """
                hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
                import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                var change = @@updateSql(id)<%
                    UPDATE example_people SET balance = balance + 1 WHERE id = #{id}
                %>;
                var failSql = @@insertSql()<%
                    INSERT INTO missing_table (id) VALUES (1)
                %>;
                """ + body;
        return this.builder.createQuery(script).execute().getData().unwrap();
    }

    private void assertBalances(int alice, int bob) throws Exception {
        try (Connection connection = this.source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT balance FROM example_people ORDER BY id")) {
            assertTrue(rows.next());
            assertEquals(alice, rows.getInt(1));
            assertTrue(rows.next());
            assertEquals(bob, rows.getInt(1));
        }
    }

    private Object applicationTransaction(TransactionCallback callback) throws Throwable {
        AtomicReference<Object> result = new AtomicReference<>();
        TranUtils.execute(new TransactionAnno(), () -> result.set(callback.execute()));
        return result.get();
    }

    private void applicationUpdate() throws Exception {
        assertTrue(TranUtils.inTrans());
        try (Connection connection = TranUtils.getConnectionProxy(this.source); var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE example_people SET balance = balance + 1 WHERE id = 2");
        }
    }
}
