package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;
import org.junit.Test;

import java.sql.Connection;
import java.util.*;

import static org.junit.Assert.*;

public class ExecuteFinal2Test extends AbstractExecuteTest {

    // line 133: timeout > 0
    @Test
    public void testStatementWithTimeout() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource d, net.hasor.dataql.sqlproc.dynamic.QueryContext c, net.hasor.dataql.sqlproc.dynamic.SqlBuilder b) {
                    b.appendSql("SELECT id FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("statementType", "statement");
            hints.setHint("timeout", "30");
            SelectConfig config = new SelectConfig(ads, hints);

            StatementExecute exec = new StatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
        }
    }

    // line 178: bindOut with context fallback
    @Test
    public void testBindOutWithContextFallback() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource d, net.hasor.dataql.sqlproc.dynamic.QueryContext c, net.hasor.dataql.sqlproc.dynamic.SqlBuilder b) {
                    b.appendSql("SELECT id, name FROM users WHERE age > 20");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "usersResult,contextOnly");
            ExecuteConfig config = new ExecuteConfig(ads, hints);

            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            data.put("contextOnly", "fromCtx");
            Object result = exec.execute(conn, hints, config, data, null, false);
            assertTrue(result instanceof Map);
            Map<?, ?> m = (Map<?, ?>) result;
            assertEquals("fromCtx", m.get("contextOnly"));
        }
    }

    // line 207: non-query statement returns update count
    @Test
    public void testNonQueryReturnsUpdateCount() throws Exception {
        try (Connection conn = newConnection()) {
            // Use a DML that doesn't match any rows
            StatementExecute exec = new StatementExecute(newQueryContext());
            SqlConfig config = newDeleteConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(0, ((Integer) result).intValue());
        }
    }

    // line 194: empty result set (rs.isLast() true)
    // H2 doesn't easily return empty result sets from SELECT without WHERE filtering
    // This is hard to test without mocking

    // Multiple result set with update counts (lines 223-242)
    @Test
    public void testMultipleResultsWithMixedTypes() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource d, net.hasor.dataql.sqlproc.dynamic.QueryContext c, net.hasor.dataql.sqlproc.dynamic.SqlBuilder b) {
                    // Multiple statements: select + update
                    b.appendSql("SELECT id, name FROM users WHERE age > 20; UPDATE users SET age=age WHERE 1=0");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "queryResult,updateResult");
            hints.setHint("statementType", "statement");
            ExecuteConfig config = new ExecuteConfig(ads, hints);

            StatementExecute exec = new StatementExecute(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            data.put("updateResult", "fallback");
            Object result = exec.execute(conn, hints, config, data, null, false);
            assertTrue(result instanceof Map);
        }
    }
}
