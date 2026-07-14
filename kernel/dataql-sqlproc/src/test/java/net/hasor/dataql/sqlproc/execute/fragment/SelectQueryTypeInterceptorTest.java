package net.hasor.dataql.sqlproc.execute.fragment;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.dataql.domain.HintsReadOnly;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dialect.PageResult;
import net.hasor.dataql.sqlproc.types.SqlArg;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.execute.RootStatement;
import net.hasor.dataql.sqlproc.execute.TestSqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInvocation;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlInfo;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SelectQueryTypeInterceptorTest extends AbstractSqlProcTest {
    @After
    public void clearInterceptor() {
        TestSqlExecutionInterceptor.clear();
    }

    @Test
    public void filtersByRegistrationPredicate() {
        ExecuteContext context = newQueryContext((sourceName, hints) -> null);
        SqlExecutionInterceptor interceptor = SqlExecutionInvocation::proceed;
        context.addInterceptor(interceptor, (type, fragmentString, hints) -> type == QueryType.Select && fragmentString.contains("users") && Boolean.TRUE.equals(hints.getHint("enabled")));

        HintsSet enabled = hints("enabled", true);
        assertTrue(context.filterInterceptors(QueryType.Select, "SELECT * FROM users", enabled).contains(interceptor));
        assertFalse(context.filterInterceptors(QueryType.Update, "UPDATE users SET age = 1", enabled).contains(interceptor));
        assertFalse(context.filterInterceptors(QueryType.Select, "SELECT * FROM roles", enabled).contains(interceptor));
        assertFalse(context.filterInterceptors(QueryType.Select, "SELECT * FROM users", hints()).contains(interceptor));
        assertTrue(context.removeInterceptor(interceptor));
        assertFalse(context.filterInterceptors(QueryType.Select, "SELECT * FROM users", enabled).contains(interceptor));
    }

    @Test
    public void observesFinalSqlAndProceeds() throws Exception {
        AtomicReference<SqlInfo> observed = new AtomicReference<>();
        TestSqlExecutionInterceptor.use(invocation -> {
            observed.set(invocation.getSqlInfo());
            return invocation.proceed();
        });
        HintsSet hints = hints(SqlHintNames.FRAGMENT_SQL_DATA_SOURCE.name(), "auditDs");
        SqlConfig config = sqlConfig(QueryType.Select, "SELECT name FROM users WHERE age = :age");

        Object result;
        try (Connection connection = newH2WithUsers()) {
            ExecuteContext ctx = newQueryContext();
            ctx.addInterceptor(new TestSqlExecutionInterceptor());
            result = new RootStatement(ctx).execute(connection, hints, config, Map.of("age", 25), null, false, ctx.filterInterceptors(config.getType(), null, hints));
        }

        assertEquals("Alice", result);
        assertEquals("auditDs", observed.get().sourceName());
        assertTrue(observed.get().hints() instanceof HintsReadOnly);
        assertEquals("auditDs", observed.get().hints().getHint(SqlHintNames.FRAGMENT_SQL_DATA_SOURCE.name()));
        assertEquals("SELECT name FROM users WHERE age = ?", observed.get().queryString());
        Object[] queryParams = observed.get().queryParams();
        assertEquals(1, queryParams.length);
        assertTrue(queryParams[0] instanceof SqlArg);
        assertEquals(25, ((SqlArg) queryParams[0]).getValue());
        try {
            observed.get().hints().setHint("test", true);
            throw new AssertionError("SqlInfo hints must be read-only.");
        } catch (UnsupportedOperationException expected) {
            assertEquals("readOnly.", expected.getMessage());
        }
    }

    @Test
    public void returnsCustomResultWithoutJdbcExecution() throws Exception {
        List<Map<String, Object>> customResult = List.of(Map.of("NAME", "intercepted"));
        TestSqlExecutionInterceptor.use(invocation -> customResult);
        SqlConfig config = sqlConfig(QueryType.Select, "SELECT * FROM table_that_does_not_exist");

        Object result;
        try (Connection connection = newH2WithUsers()) {
            ExecuteContext ctx = newQueryContext();
            ctx.addInterceptor(new TestSqlExecutionInterceptor());
            HintsSet hints = hints();
            result = new RootStatement(ctx).execute(connection, hints, config, Collections.emptyMap(), null, false, ctx.filterInterceptors(config.getType(), null, hints));
        }

        assertSame(customResult, result);
    }

    @Test
    public void interceptsPagingCountSql() throws Exception {
        AtomicReference<SqlInfo> countInfo = new AtomicReference<>();
        TestSqlExecutionInterceptor.use(invocation -> {
            if (invocation.getSqlInfo().queryString().toUpperCase().contains("COUNT(")) {
                countInfo.set(invocation.getSqlInfo());
                return 99L;
            }
            return invocation.proceed();
        });
        PageObject page = new PageObject(0, 1, -1);
        page.refreshTotalCount();

        Object result;
        try (Connection connection = newH2WithUsers()) {
            ExecuteContext ctx = newQueryContext();
            ctx.addInterceptor(new TestSqlExecutionInterceptor());
            HintsSet hints = hints();
            SqlConfig config = sqlConfig(QueryType.Select, "SELECT id, name, age FROM users");
            result = new RootStatement(ctx).execute(connection, hints, config, Collections.emptyMap(), page, true, ctx.filterInterceptors(config.getType(), null, hints));
        }

        assertTrue(result instanceof PageResult);
        assertEquals(99L, ((PageResult<?>) result).getTotalCount());
        assertTrue(countInfo.get().queryString().toUpperCase().contains("COUNT("));
    }
}
