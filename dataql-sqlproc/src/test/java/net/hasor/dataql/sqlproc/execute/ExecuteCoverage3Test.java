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

public class ExecuteCoverage3Test extends AbstractExecuteTest {

    // PreparedStatementExecute with DML (non-DQL)
    @Test
    public void testPreparedDmlReturnsUpdateCount() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newUpdateConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(1, ((Integer) result).intValue());
        }
    }

    // PreparedStatementExecute: DQL with paging, refresh total count
    @Test
    public void testPagingWithRefreshTotalCount() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newSelectConfig(null); // all 3 rows
            MockPage3 page = new MockPage3(1);
            page.setTotalCount(-1); // triggers count
            page.refreshTotalCount();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), page, true);
            assertTrue(result instanceof PageResult);
            assertEquals(3, ((PageResult<?>) result).getTotalCount());
        }
    }

    // StatementExecute with DML config (fetchResult DmlConfig path)
    @Test
    public void testStatementDmlReturnsUpdateCount() throws Exception {
        try (Connection conn = newConnection()) {
            StatementExecute exec = new StatementExecute(newQueryContext());
            SqlConfig config = newUpdateConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(1, ((Integer) result).intValue());
        }
    }

    // PreparedStatementExecute: non-query (execute returns false)
    @Test
    public void testPreparedNonQueryReturnsUpdateCount() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newDeleteConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(0, ((Integer) result).intValue());
        }
    }

    // Paging with non-zero totalCount (no count SQL)
    @Test
    public void testPagingWithPresetTotalCount() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newSelectConfig(null);
            MockPage3 page = new MockPage3(1);
            page.setTotalCount(50); // preset, no count needed
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), page, false);
            assertTrue(result instanceof List);
        }
    }

    // ExecuteConfig (non-DqlConfig) with bindOut
    @Test
    public void testExecuteConfigBindOutFetchMultiple() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users WHERE age > 20");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "usersResult");
            ExecuteConfig config = new ExecuteConfig(ads, hints);

            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof Map);
        }
    }

    // DqlConfig bindOut with context fallback
    @Test
    public void testDqlBindOutWithContextFallback() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "result,fromContext");
            SelectConfig config = new SelectConfig(ads, hints);

            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            data.put("fromContext", "ctxValue");
            Object result = exec.execute(conn, hints, config, data, null, false);
            assertTrue(result instanceof Map);
            assertEquals("ctxValue", ((Map<?, ?>) result).get("fromContext"));
        }
    }

    // ---
    private static class MockPage3 implements Page {
        private long ps, cp, po, tc; private boolean rf;
        MockPage3(long ps) { this.ps = ps; }
        public long getPageSize() { return ps; } public void setPageSize(long v) { ps = v; }
        public long getCurrentPage() { return cp; } public void setCurrentPage(long v) { cp = v; }
        public long getPageNumberOffset() { return po; } public void setPageNumberOffset(long v) { po = v; }
        public long getFirstRecordPosition() { return cp * ps; }
        public long getTotalPage() { return ps > 0 ? (tc + ps - 1) / ps : 0; }
        public long getTotalCount() { return tc; } public void setTotalCount(long v) { tc = v; }
        public void refreshTotalCount() { rf = true; } public boolean isRefreshTotalCount() { return rf; }
    }
}
