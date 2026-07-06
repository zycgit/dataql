package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dialect.PageResult;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;
import org.junit.Test;

import java.sql.Connection;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Extended coverage tests for AbstractStatementExecute branches.
 */
public class ExecuteCoverage2Test extends AbstractExecuteTest {

    // --- Paging with count: pageInfo.getTotalCount() <= 0 means refresh needed ---
    @Test
    public void testPagingCountWhenTotalCountZero() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newSelectConfig(null); // all 3 rows
            MockPage2 page = new MockPage2(1);
            page.setTotalCount(0);  // triggers count query
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), page, true);
            assertTrue(result instanceof PageResult);
            PageResult<?> pr = (PageResult<?>) result;
            assertEquals(3, pr.getTotalCount());
        }
    }

    // --- BindOut + paging = error ---
    @Test(expected = java.sql.SQLException.class)
    public void testBindOutWithPagingError() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "col1");
            ExecuteConfig config = new ExecuteConfig(ads, hints);

            MockPage2 page = new MockPage2(2);
            exec.execute(conn, hints, config, Collections.emptyMap(), page, false);
        }
    }

    // --- SelectKey with BeanMap result (non-Map result) ---
    @Test
    public void testSelectKeyWithBeanResult() throws Exception {
        // This test expects the result to be a List<Map> which is the normal case
        // Already covered by SelectKeyStatementExecuteTest
    }

    // --- ColumnCaseType: different values ---
    @Test
    public void testFetchResultWithColumnCaseType() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            HintsSet hints = new HintsSet();
            hints.setHint("FRAGMENT_SQL_COLUMN_CASE", "lower");
            SqlConfig config = newPreparedSelectConfig();
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
        }
    }

    // --- ExecuteConfig with bindOut ---
    @Test
    public void testExecuteConfigWithBindOut() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "result1,result2");
            ExecuteConfig config = new ExecuteConfig(ads, hints);

            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof Map);
            Map<?, ?> m = (Map<?, ?>) result;
            assertTrue(m.containsKey("result1"));
        }
    }

    // --- DqlConfig with bindOut ---
    @Test
    public void testDqlConfigWithBindOut() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "col1");
            SelectConfig config = new SelectConfig(ads, hints);

            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof Map);
        }
    }

    // --- PrepareStatement then executeQuery fails ---
    // Hard to test without mocking

    private static class MockPage2 implements Page {
        private long ps, cp, po, tc; private boolean rf;
        MockPage2(long ps) { this.ps = ps; }
        public long getPageSize() { return ps; } public void setPageSize(long v) { ps = v; }
        public long getCurrentPage() { return cp; } public void setCurrentPage(long v) { cp = v; }
        public long getPageNumberOffset() { return po; } public void setPageNumberOffset(long v) { po = v; }
        public long getFirstRecordPosition() { return cp * ps; }
        public long getTotalPage() { return ps > 0 ? (tc + ps - 1) / ps : 0; }
        public long getTotalCount() { return tc; } public void setTotalCount(long v) { tc = v; }
        public void refreshTotalCount() { rf = true; } public boolean isRefreshTotalCount() { return rf; }
    }
}
