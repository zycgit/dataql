package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class PreparedStatementExecuteTest extends AbstractExecuteTest {

    @Test
    public void testSelectQuery() throws SQLException {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newPreparedSelectConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
            assertEquals(1, ((List<?>) result).size());
            Map<?, ?> row = (Map<?, ?>) ((List<?>) result).get(0);
            assertEquals("Alice", row.get("NAME"));
        }
    }

    @Test
    public void testInsert() throws SQLException {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newPreparedInsertConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(1, ((Integer) result).intValue());
        }
    }

    @Test
    public void testNonQueryStatement() throws SQLException {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newDeleteConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(0, ((Integer) result).intValue());
        }
    }

    @Test
    public void testWithHintTimeout() throws SQLException {
        try (Connection conn = newConnection()) {
            HintsSet hints = new HintsSet();
            hints.setHint("timeout", "5");

            // use a select config with Prepared type
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newSelectConfig(null); // defaults to Prepared
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertNotNull(result);
        }
    }

    @Test
    public void testWithPageResult() throws SQLException {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newPreparedSelectConfig(); // WHERE name = 'Alice' -> 1 row
            Page page = new MockPage(10);
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), page, true);
            assertTrue(result instanceof net.hasor.dataql.sqlproc.dialect.PageResult);
            net.hasor.dataql.sqlproc.dialect.PageResult<?> pr = (net.hasor.dataql.sqlproc.dialect.PageResult<?>) result;
            assertEquals(1, pr.getData().size());
        }
    }

    // ---
    private static class MockPage implements Page {
        private long ps, cp, po, tc; private boolean rf;
        MockPage(long ps) { this.ps = ps; }
        public long getPageSize() { return ps; } public void setPageSize(long v) { ps = v; }
        public long getCurrentPage() { return cp; } public void setCurrentPage(long v) { cp = v; }
        public long getPageNumberOffset() { return po; } public void setPageNumberOffset(long v) { po = v; }
        public long getFirstRecordPosition() { return cp * ps; }
        public long getTotalPage() { return ps > 0 ? (tc + ps - 1) / ps : 0; }
        public long getTotalCount() { return tc; } public void setTotalCount(long v) { tc = v; }
        public void refreshTotalCount() { rf = true; } public boolean isRefreshTotalCount() { return rf; }
    }
}
