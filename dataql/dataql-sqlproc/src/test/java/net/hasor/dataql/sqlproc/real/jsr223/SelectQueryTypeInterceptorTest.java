/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.real.jsr223;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.host.jsr223.QueryScriptContext;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.execute.TestSqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlInfo;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectQueryTypeInterceptorTest extends AbstractSqlProcTest {
    private String jdbcUrl;

    @Before
    public void setupUsers() throws Exception {
        this.jdbcUrl = "jdbc:h2:mem:interceptor_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        try (Connection conn = rawConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), age INT)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Alice', 25)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Bob', 30)");
        }
    }

    @After
    public void clearInterceptor() {
        TestSqlExecutionInterceptor.clear();
    }

    @Test
    public void dataqlFragmentObservesFinalSqlAndHints() throws Exception {
        AtomicReference<SqlInfo> observed = new AtomicReference<>();
        TestSqlExecutionInterceptor.use(invocation -> {
            observed.set(invocation.getSqlInfo());
            return invocation.proceed();
        });
        Object result = unwrap(eval("""
                hint FRAGMENT_SQL_DATA_SOURCE = "auditDs"
                var loadUser = @@selectSql(age)<%
                    SELECT name FROM users WHERE age = :age
                %>;
                return loadUser(${age});
                """, Map.of("age", 25), context -> context.addInterceptor(new TestSqlExecutionInterceptor())));

        assertEquals("Alice", result);
        assertEquals("auditDs", observed.get().sourceName());
        assertEquals("auditDs", observed.get().hints().getHint(SqlHintNames.FRAGMENT_SQL_DATA_SOURCE.name()));
        assertEquals("SELECT name FROM users WHERE age = ?", normalizeSql(observed.get().queryString()));
    }

    @Test
    public void dataqlFragmentCanReturnCustomResultWithoutJdbcExecution() throws Exception {
        TestSqlExecutionInterceptor.use(invocation -> Map.of("NAME", "intercepted"));
        Object result = unwrap(eval("""
                var loadUser = @@selectSql()<%
                    SELECT * FROM table_that_does_not_exist
                %>;
                var user = loadUser();
                return user.NAME;
                """, context -> context.addInterceptor(new TestSqlExecutionInterceptor())));

        assertEquals("intercepted", result);
    }

    @Test
    public void dataqlFragmentFiltersInterceptorByFragmentStringAndHints() throws Exception {
        AtomicReference<SqlInfo> observed = new AtomicReference<>();
        TestSqlExecutionInterceptor.use(invocation -> {
            observed.set(invocation.getSqlInfo());
            return invocation.proceed();
        });
        Object result = unwrap(eval("""
                hint enabled = true
                var loadUser = @@selectSql(name)<%
                    SELECT name FROM users WHERE name = :name
                %>;
                return loadUser(${name});
                """, Map.of("name", "Bob"), context -> {
            context.addInterceptor(new TestSqlExecutionInterceptor(), (type, fragmentString, hints) -> {
                return type == QueryType.Select && fragmentString.contains("FROM users") && Boolean.TRUE.equals(hints.getHint("enabled"));
            });
        }));

        assertEquals("Bob", result);
        assertTrue(observed.get().queryString().contains("FROM users"));
    }

    private QueryResult eval(String dataql, Consumer<ExecuteContext> customizer) throws Exception {
        return eval(dataql, Map.of(), customizer);
    }

    private QueryResult eval(String dataql, Map<String, Object> params, Consumer<ExecuteContext> customizer) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("dataql");
        QueryScriptContext context = (QueryScriptContext) engine.getContext();
        context.addAttachment(ConnectionProvider.class, (sourceName, hints) -> rawConnection());
        ExecuteContext queryContext = context.getAttachment(ExecuteContext.class);
        customizer.accept(queryContext);
        Bindings bindings = engine.createBindings();
        bindings.putAll(params);
        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return (QueryResult) engine.eval(dataql, context);
    }

    private Connection rawConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
    }

    private Object unwrap(QueryResult queryResult) {
        DataModel data = queryResult.getData();
        return data.unwrap();
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
