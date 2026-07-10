package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MixedResultTest extends AbstractSqlProcTest {

    // line 133: timeout > 0
    @Test
    public void statementTimeout() throws Exception {
        HintsSet hints = hints();
        hints.setHint("statementType", "statement");
        hints.setHint("timeout", "30");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    // line 178: bindOut with context fallback
    @Test
    public void bindOutContext() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "usersResult,contextOnly");
        SqlConfig config = sqlConfig(QueryType.Execute, hints, "SELECT id, name FROM users WHERE age > 20");
        Map<String, Object> data = new HashMap<>();
        data.put("contextOnly", "fromCtx");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, data, null, false);

            assertTrue(result instanceof Map);
            Map<?, ?> m = (Map<?, ?>) result;
            assertEquals("fromCtx", m.get("contextOnly"));
        }
    }

    // line 207: non-query statement returns update count
    @Test
    public void nonQueryCount() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = deleteNoUserConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(0, ((Integer) result).intValue());
        }
    }

    // line 194: empty result set (rs.isLast() true)
    // H2 doesn't easily return empty result sets from SELECT without WHERE filtering
    // This is hard to test without mocking

    // Multiple result set with update counts (lines 223-242)
    @Test
    public void mixedMultipleResults() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "queryResult,updateResult");
        hints.setHint("statementType", "statement");
        SqlConfig config = sqlConfig(QueryType.Execute, hints, "SELECT id, name FROM users WHERE age > 20; UPDATE users SET age=age WHERE 1=0");
        Map<String, Object> data = new HashMap<>();
        data.put("updateResult", "fallback");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, data, null, false);

            assertTrue(result instanceof Map);
        }
    }

}
