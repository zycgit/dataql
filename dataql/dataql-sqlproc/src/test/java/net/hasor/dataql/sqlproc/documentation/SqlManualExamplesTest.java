/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.documentation;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.dynamic.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContextImpl;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionProvider;
import net.hasor.dataql.util.JsonUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Executes the SQL manual's complete scripts against isolated H2 databases. */
@RunWith(Parameterized.class)
public class SqlManualExamplesTest {
    private final String scenario;

    public SqlManualExamplesTest(String scenario) {
        this.scenario = scenario;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[] scenarios() {
        return new Object[] { "query", "crud", "batch", "parameters", "position", "rules", "in-rule", "xml", "xml-update", "xml-choose", "results", "page", "select-key", "call", "bind-out", "transaction", "rollback", "custom-type", "custom-rule", "macro", "rule-braces", "xml-trim", "xml-or", "page-empty", "page-navigation" };
    }

    @Test
    public void documentedScriptHasTheExpectedDatabaseEffect() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:manual_" + UUID.randomUUID());
        try (Connection keeper = source.getConnection()) {
            this.initialize(keeper);
            ConnectionProvider raw = (name, hints) -> source.getConnection();
            try (TransactionProvider provider = new TransactionProvider(raw)) {
                HostConfiguration host = new HostConfiguration();
                host.addAttachment(ConnectionProvider.class, provider);
                ExecuteContext context = host.getAttachment(ExecuteContext.class);
                ((ExecuteContextImpl) context).addMacro("adult", "age >= 18");
                RuleRegistry.DEFAULT.register("manualEnabled", new EnabledRule());
                List<String> statements = new ArrayList<>();
                context.addInterceptor(invocation -> {
                    statements.add(invocation.getSqlInfo().queryString());
                    return invocation.proceed();
                });
                Query query = new QueryManager(host).newBuilder().createQuery(this.resource(this.scenario + ".dql"));
                if ("call".equals(this.scenario)) {
                    assertFalse(keeper.getMetaData().supportsStoredProcedures());
                    Exception failure = assertThrows(Exception.class, query::execute);
                    Throwable cause = failure;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    assertTrue(cause instanceof UnsupportedOperationException);
                    assertTrue(statements.isEmpty());
                    return;
                } else if ("rollback".equals(this.scenario)) {
                    assertThrows(Exception.class, query::execute);
                    assertEquals(100, this.balance(keeper, 1));
                    assertEquals(100, this.balance(keeper, 2));
                } else {
                    Object result = query.execute().getData().unwrap();
                    Object expected = JsonUtils.readValue(this.resource(this.scenario + ".json"), Object.class);
                    this.assertData(expected, result);
                    if ("transaction".equals(this.scenario)) {
                        assertEquals(90, this.balance(keeper, 1));
                        assertEquals(110, this.balance(keeper, 2));
                    }
                }
                assertFalse("The script must reach JDBC", statements.isEmpty());
                if ("page".equals(this.scenario) || "page-empty".equals(this.scenario)) {
                    assertEquals("Count and page SQL are intercepted separately", 2, statements.size());
                } else if ("page-navigation".equals(this.scenario)) {
                    assertEquals(4, statements.size());
                }
            }
        }
    }

    private void initialize(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE people (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(100), age INT, enabled INT DEFAULT 1)");
            statement.execute("INSERT INTO people(name, age) VALUES ('Alice',25),('Bob',30)");
            statement.execute("CREATE SEQUENCE people_ids START WITH 100");
            statement.execute("CREATE TABLE accounts (id INT PRIMARY KEY, balance INT)");
            statement.execute("INSERT INTO accounts VALUES (1,100),(2,100)");
        }
    }

    private String resource(String name) throws Exception {
        try (InputStream input = this.getClass().getResourceAsStream("/sql-manual/" + name)) {
            assertNotNull(name, input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int balance(Connection connection, int id) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT balance FROM accounts WHERE id = " + id)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private void assertData(Object expected, Object actual) {
        if (expected instanceof Number && actual instanceof Number) {
            assertEquals(0, new BigDecimal(expected.toString()).compareTo(new BigDecimal(actual.toString())));
        } else if (expected instanceof List<?> expectedList && actual instanceof List<?> actualList) {
            assertEquals(expectedList.size(), actualList.size());
            for (int index = 0; index < expectedList.size(); index++) {
                this.assertData(expectedList.get(index), actualList.get(index));
            }
        } else if (expected instanceof Map<?, ?> expectedMap && actual instanceof Map<?, ?> actualMap) {
            assertEquals(expectedMap.keySet(), actualMap.keySet());
            for (Object key : expectedMap.keySet()) {
                this.assertData(expectedMap.get(key), actualMap.get(key));
            }
        } else {
            assertEquals(expected, actual);
        }
    }
}
