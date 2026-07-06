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
 * Tests targeting remaining coverage gaps.
 */
public class ExecuteCoverageTest extends AbstractExecuteTest {

    // --- AbstractStatementExecute: paging with count query ---
    @Test
    public void testPagingWithCount() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newPreparedSelectConfig(); // WHERE name='Alice' -> 1 row
            Page page = new MockPage(2);
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), page, true);
            assertTrue(result instanceof PageResult);
        }
    }

    // --- AbstractStatementExecute: paging without pageResult ---
    @Test
    public void testPagingWithoutPageResult() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newPreparedSelectConfig();
            Page page = new MockPage(2);
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), page, false);
            assertTrue(result instanceof List);
        }
    }

    // --- AbstractStatementExecute: with timeout hint ---
    @Test
    public void testWithTimeoutHint() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            HintsSet hints = new HintsSet();
            hints.setHint("timeout", "10");
            SqlConfig config = newPreparedSelectConfig();
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertNotNull(result);
        }
    }

    // --- AbstractStatementExecute: MergedMap data ---
    @Test
    public void testWithMergedMapData() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            MergedMap<String, Object> data = new MergedMap<>();
            data.put("extra", "value");
            SqlConfig config = newPreparedSelectConfig();
            Object result = exec.execute(conn, new HintsSet(), config, data, null, false);
            assertNotNull(result);
        }
    }

    // --- AbstractStatementExecute: non-DQL fetchResult (DML update count) ---
    @Test
    public void testDmlUpdateCount() throws Exception {
        try (Connection conn = newConnection()) {
            StatementExecute exec = new StatementExecute(newQueryContext());
            SqlConfig config = newUpdateConfig();
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(1, ((Integer) result).intValue());
        }
    }

    // --- AbstractStatementExecute: DQL with fetchSize ---
    @Test
    public void testDqlWithFetchSize() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users WHERE age > 20");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("fetchSize", "100");
            SelectConfig config = new SelectConfig(ads, hints);

            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
            assertEquals(3, ((List<?>) result).size());
        }
    }

    // --- AbstractStatementExecute: DQL with ResultSetType ---
    @Test
    public void testDqlWithScrollSensitive() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("resultSetType", "scroll_sensitive");
            SelectConfig config = new SelectConfig(ads, hints);

            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
        }
    }

    // --- AbstractStatementExecute: DQL with scroll_insensitive ---
    @Test
    public void testDqlWithScrollInsensitive() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("resultSetType", "scroll_insensitive");
            SelectConfig config = new SelectConfig(ads, hints);

            StatementExecute exec = new StatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
        }
    }

    // --- AbstractStatementExecute: StatementExecute with DQL ---
    @Test
    public void testStatementExecuteDql() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("statementType", "statement");
            SelectConfig config = new SelectConfig(ads, hints);

            StatementExecute exec = new StatementExecute(newQueryContext());
            Object result = exec.execute(conn, hints, config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
        }
    }

    // --- Row counting path with totalCount already set ---
    @Test
    public void testPagingWithExistingTotalCount() throws Exception {
        try (Connection conn = newConnection()) {
            PreparedStatementExecute exec = new PreparedStatementExecute(newQueryContext());
            SqlConfig config = newPreparedSelectConfig();
            MockPage page = new MockPage(2);
            page.setTotalCount(100);
            Object result = exec.execute(conn, new HintsSet(), config, Collections.emptyMap(), page, true);
            assertTrue(result instanceof PageResult);
            PageResult<?> pr = (PageResult<?>) result;
            assertEquals(100, pr.getTotalCount());
        }
    }

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
