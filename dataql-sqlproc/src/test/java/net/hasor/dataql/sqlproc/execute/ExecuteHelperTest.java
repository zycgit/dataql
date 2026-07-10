package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dynamic.SqlArg;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecuteHelperTest {

    @Test
    public void usingPageNull() {
        assertFalse(ExecuteHelper.usingPage(null));
    }

    @Test
    public void usingPageZero() {
        assertFalse(ExecuteHelper.usingPage(new PageObject(0, 0)));
    }

    @Test
    public void usingPageNegative() {
        assertFalse(ExecuteHelper.usingPage(new PageObject(0, -1)));
    }

    @Test
    public void usingPagePositive() {
        assertTrue(ExecuteHelper.usingPage(new PageObject(0, 10)));
    }

    @Test
    public void formatNull() {
        assertTrue(ExecuteHelper.fmtBoundSql(null).toString().contains("Empty"));
    }

    @Test
    public void formatSimple() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM u WHERE id = ?", new Object[] { 1 });
        String s = ExecuteHelper.fmtBoundSql(sql).toString();
        assertTrue(s.contains("SELECT") && s.contains("1"));
    }

    @Test
    public void formatNullArg() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[] { null });
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("null"));
    }

    @Test
    public void formatStringArg() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t WHERE n = ?", new Object[] { "hi" });
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("'hi'"));
    }

    @Test
    public void formatLongString() {
        char[] cs = new char[3000];
        for (int i = 0; i < cs.length; i++)
            cs[i] = 'x';
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[] { new String(cs) });
        String s = ExecuteHelper.fmtBoundSql(sql).toString();
        assertTrue(s.length() < 2200 && s.contains("...'"));
    }

    @Test
    public void formatSqlArg() {
        SqlArg a = SqlArg.valueOf("hello");
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[] { a });
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("hello"));
    }

    @Test
    public void formatPageArg() {
        PageObject p = new PageObject(0, 10);
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT * FROM t", new Object[] { p });
        assertTrue(ExecuteHelper.fmtBoundSql(sql).toString().contains("pageSize=10"));
    }

    @Test
    public void formatMultilineSql() {
        BoundSql sql = new BoundSql.BoundSqlObj("SELECT\n  *\nFROM\n  t", new Object[0]);
        assertFalse(ExecuteHelper.fmtBoundSql(sql).toString().contains("\n"));
    }
}
