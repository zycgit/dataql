package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class StatementExecuteTest extends AbstractExecuteTest {

    @Test
    public void testSelectQuery() throws SQLException {
        try (Connection conn = newConnection()) {
            StatementExecute exec = new StatementExecute(newQueryContext());
            SqlConfig config = newSelectConfig(StatementType.Statement);
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
            assertEquals(3, ((List<?>) result).size());
        }
    }

    @Test
    public void testInsert() throws SQLException {
        try (Connection conn = newConnection()) {
            StatementExecute exec = new StatementExecute(newQueryContext());
            SqlConfig config = newInsertConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(1, ((Integer) result).intValue());
        }
    }

    @Test
    public void testDelete() throws SQLException {
        try (Connection conn = newConnection()) {
            StatementExecute exec = new StatementExecute(newQueryContext());
            SqlConfig config = newDeleteConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(0, ((Integer) result).intValue());
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPagingNotSupported() throws SQLException {
        try (Connection conn = newConnection()) {
            StatementExecute exec = new StatementExecute(newQueryContext());
            Page page = new MockPage(10);
            exec.execute(conn, new HintsSet(), newSelectConfig(StatementType.Statement), Collections.emptyMap(), page, false);
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
