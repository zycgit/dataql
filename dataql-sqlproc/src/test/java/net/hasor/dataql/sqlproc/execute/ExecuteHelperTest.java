package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.SqlArg;
import net.hasor.dataql.sqlproc.dynamic.SqlMode;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExecuteHelperTest {

    @Test
    public void testUsingPageNull() {
        assertFalse(ExecuteHelper.usingPage(null));
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

    @Test public void testUsingPageZero() { assertFalse(ExecuteHelper.usingPage(new MockPage(0))); }
    @Test public void testUsingPageNeg() { assertFalse(ExecuteHelper.usingPage(new MockPage(-1))); }
    @Test public void testUsingPagePos() { assertTrue(ExecuteHelper.usingPage(new MockPage(10))); }

    @Test public void testFmtNull() {
        assertTrue(ExecuteHelper.fmtBoundSql(null).toString().contains("Empty"));
    }

    @Test public void testFmtSimple() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM u WHERE id = ?", new Object[]{1});
        String s = ExecuteHelper.fmtBoundSql(sql).toString();
        assertTrue(s.contains("SELECT") && s.contains("1"));
    }

    @Test public void testFmtNullArg() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[]{null});
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("null"));
    }

    @Test public void testFmtStrArg() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t WHERE n = ?", new Object[]{"hi"});
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("'hi'"));
    }

    @Test public void testFmtLongStr() {
        char[] cs = new char[3000];
        for (int i = 0; i < cs.length; i++) cs[i] = 'x';
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[]{new String(cs)});
        String s = ExecuteHelper.fmtBoundSql(sql).toString();
        assertTrue(s.length() < 2200 && s.contains("...'"));
    }

    @Test public void testFmtSqlArg() {
        SqlArg a = SqlArg.valueOf("hello");
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[]{a});
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("hello"));
    }

    @Test public void testFmtPageArg() {
        MockPage p = new MockPage(10);
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[]{p});
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("pageSize=10"));
    }

    @Test public void testFmtMultiLine() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT\n  *\nFROM\n  t", new Object[0]);
        assertFalse(ExecuteHelper.fmtBoundSql(sql).toString().contains("\n"));
    }
}
