package net.hasor.dataql.sqlproc.execute.fragment;

import java.util.*;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExecuteQueryTypeFragmentTest extends AbstractSqlProcTest {

    /** Supplier that creates fresh H2 connections per call */
    private ExecuteFragmentProcess newFragment() {
        return new ExecuteFragmentProcess(newQueryContext(name -> newH2WithUsers()));
    }

    // ----------------------------------------------------------------
    // inferQueryType
    // ----------------------------------------------------------------

    @Test
    public void inferSelect() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.inferQueryType("select * from users"));
    }

    @Test
    public void inferSelectUpper() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.inferQueryType("SELECT * FROM users"));
    }

    @Test
    public void inferWith() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.inferQueryType("with cte as (select 1) select * from cte"));
    }

    @Test
    public void inferInsert() {
        assertEquals(QueryType.Insert, ExecuteFragmentProcess.inferQueryType("insert into users values (1)"));
    }

    @Test
    public void inferReplace() {
        assertEquals(QueryType.Insert, ExecuteFragmentProcess.inferQueryType("replace into users values (1)"));
    }

    @Test
    public void inferUpdate() {
        assertEquals(QueryType.Update, ExecuteFragmentProcess.inferQueryType("update users set name='x'"));
    }

    @Test
    public void inferDelete() {
        assertEquals(QueryType.Delete, ExecuteFragmentProcess.inferQueryType("delete from users"));
    }

    @Test
    public void inferCall() {
        assertEquals(QueryType.Call, ExecuteFragmentProcess.inferQueryType("call myproc(1)"));
    }

    @Test
    public void inferCallBrace() {
        assertEquals(QueryType.Call, ExecuteFragmentProcess.inferQueryType("{call myproc(?, ?)}"));
    }

    @Test
    public void inferExec() {
        assertEquals(QueryType.Call, ExecuteFragmentProcess.inferQueryType("exec myproc"));
    }

    @Test
    public void inferDdl() {
        assertEquals(QueryType.Execute, ExecuteFragmentProcess.inferQueryType("create table t(id int)"));
    }

    @Test
    public void inferEmpty() {
        assertEquals(QueryType.Execute, ExecuteFragmentProcess.inferQueryType(""));
    }

    @Test
    public void inferBlank() {
        assertEquals(QueryType.Execute, ExecuteFragmentProcess.inferQueryType("   "));
    }

    @Test
    public void inferLineComment() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.inferQueryType("-- comment\nselect * from t"));
    }

    @Test
    public void inferBlockComment() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.inferQueryType("/* single line */ select * from t"));
    }

    @Test
    public void inferMultilineComment() {
        // multiline comment on same line as trailing text
        assertEquals(QueryType.Select, ExecuteFragmentProcess.inferQueryType("/* comment */ select * from t"));
    }

    // ----------------------------------------------------------------
    // parseExplicitType (@@ prefix)
    // ----------------------------------------------------------------

    @Test
    public void explicitInsert() {
        assertEquals(QueryType.Insert, ExecuteFragmentProcess.parseExplicitType("@@insert into users values (1)"));
    }

    @Test
    public void explicitUpdate() {
        assertEquals(QueryType.Update, ExecuteFragmentProcess.parseExplicitType("@@update users set x=1"));
    }

    @Test
    public void explicitDelete() {
        assertEquals(QueryType.Delete, ExecuteFragmentProcess.parseExplicitType("@@delete from users"));
    }

    @Test
    public void explicitSelect() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.parseExplicitType("@@select * from users"));
    }

    @Test
    public void explicitExecute() {
        assertEquals(QueryType.Execute, ExecuteFragmentProcess.parseExplicitType("@@execute create table t"));
    }

    @Test
    public void explicitCall() {
        assertEquals(QueryType.Call, ExecuteFragmentProcess.parseExplicitType("@@call myproc(1)"));
    }

    @Test
    public void explicitCaseInsensitive() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.parseExplicitType("@@SELECT * FROM users"));
    }

    @Test
    public void explicitLeadingBlank() {
        assertEquals(QueryType.Select, ExecuteFragmentProcess.parseExplicitType("  @@select * FROM users"));
    }

    @Test
    public void explicitOnlyLeadingPrefix() {
        assertNull(ExecuteFragmentProcess.parseExplicitType("select '@@insert' from users"));
    }

    @Test
    public void explicitNoPrefix() {
        assertNull(ExecuteFragmentProcess.parseExplicitType("select * from users"));
    }

    @Test
    public void explicitNull() {
        assertNull(ExecuteFragmentProcess.parseExplicitType(null));
    }

    @Test
    public void explicitEmptyAt() {
        assertNull(ExecuteFragmentProcess.parseExplicitType("@@"));
    }

    // ----------------------------------------------------------------
    // runFragment - CRUD
    // ----------------------------------------------------------------

    @Test
    public void runSelect() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "SELECT * FROM users");
        assertTrue(result instanceof List);
        assertEquals(3, ((List<?>) result).size());
    }

    @Test
    public void runInsert() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "INSERT INTO users (name, age) VALUES ('Test', 99)");
        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void runUpdate() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "UPDATE users SET age = 100 WHERE name = 'Alice'");
        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void runDelete() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "DELETE FROM users WHERE id > 100");
        assertEquals(0, ((Integer) result).intValue());
    }

    @Test
    public void runWithParams() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");
        Object result = f.runFragment(new HintsSet(), params, "SELECT * FROM users WHERE name = :name");
        assertTrue(result instanceof Map);
        assertEquals("Alice", ((Map<?, ?>) result).get("NAME"));
    }

    @Test
    public void emptyResultSet() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "SELECT * FROM users WHERE 1 = 0");
        assertNull(result);
    }

    @Test
    public void multiLineSql() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        f.runFragment(new HintsSet(), Collections.emptyMap(), "SELECT\n  id,\n  name\nFROM\n  users");
    }

    // ----------------------------------------------------------------
    // runFragment - paging
    // ----------------------------------------------------------------

    @Test
    public void runSelectPagingLazy() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");

        Object result = f.runFragment(hints, Collections.emptyMap(), "SELECT * FROM users");
        assertTrue("paging select should return LazyPageQuery", result instanceof PageQuery);
    }

    @Test
    public void runInsertPagingNoLazy() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");

        Object result = f.runFragment(hints, Collections.emptyMap(), "INSERT INTO users (name, age) VALUES ('X', 1)");
        assertFalse("insert with paging should NOT be lazy", result instanceof PageQuery);
        assertEquals(1, ((Integer) result).intValue());
    }

    // ----------------------------------------------------------------
    // LazyPageQuery
    // ----------------------------------------------------------------

    @Test
    public void lazyPageSetPageThenData() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");

        PageQuery q = (PageQuery) f.runFragment(hints, Collections.emptyMap(), "SELECT * FROM users");
        q.setPageInfo(mapOf("pageSize", 1, "currentPage", 1));
        List<?> data = (List<?>) q.data();
        assertEquals(1, data.size());
    }

    @Test
    public void lazyPageInfo() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");

        PageQuery q = (PageQuery) f.runFragment(hints, Collections.emptyMap(), "SELECT * FROM users");
        q.setPageInfo(mapOf("pageSize", 10, "currentPage", 1));
        Map<String, Object> info = q.pageInfo();
        assertTrue(info.containsKey("totalCount"));
        assertTrue(info.containsKey("pageSize"));
    }

    @Test
    public void lazyPageNextPage() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");

        PageQuery q = (PageQuery) f.runFragment(hints, Collections.emptyMap(), "SELECT * FROM users");
        q.setPageInfo(mapOf("pageSize", 1));
        q.firstPage();
        assertEquals(1, ((List<?>) q.data()).size());
        q.nextPage();
        assertEquals(1, ((List<?>) q.data()).size());
    }

    @Test
    public void lazyPageNoSizeReturnsAll() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");

        PageQuery q = (PageQuery) f.runFragment(hints, Collections.emptyMap(), "SELECT * FROM users");
        assertEquals(3, ((List<?>) q.data()).size());
    }

    // ----------------------------------------------------------------
    // batchRunFragment
    // ----------------------------------------------------------------

    @Test
    public void batchRun() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        List<Map<String, Object>> params = Arrays.asList(Collections.emptyMap(), Collections.emptyMap());
        List<Object> results = f.batchRunFragment(new HintsSet(), params, "SELECT * FROM users");
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof List);
    }

    @Test
    public void batchRunPaging() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");

        List<Map<String, Object>> params = Arrays.asList(Collections.emptyMap(), Collections.emptyMap());
        List<Object> results = f.batchRunFragment(hints, params, "SELECT * FROM users");
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof PageQuery);
    }

    // ----------------------------------------------------------------
    // config caching
    // ----------------------------------------------------------------

    @Test
    public void configCaching() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        String sql = "SELECT * FROM users";
        f.runFragment(new HintsSet(), Collections.emptyMap(), sql);
        f.runFragment(new HintsSet(), Collections.emptyMap(), sql);
        f.runFragment(new HintsSet(), Collections.emptyMap(), sql);
    }

    // ----------------------------------------------------------------
    // edge cases
    // ----------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void nullQueryContext() {
        new ExecuteFragmentProcess(null);
    }

    @Test
    public void sqlWithLineComment() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "-- get all users\nSELECT * FROM users");
        assertTrue(result instanceof List);
    }

    // ----------------------------------------------------------------
    // @@ explicit type integration
    // ----------------------------------------------------------------

    @Test
    public void runExplicitInsert() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "@@insert INSERT INTO users (name, age) VALUES ('E1', 10)");
        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void runExplicitSelect() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        Object result = f.runFragment(new HintsSet(), Collections.emptyMap(), "@@select SELECT * FROM users");
        assertTrue(result instanceof List);
    }

    @Test
    public void explicitTypePaging() throws Throwable {
        ExecuteFragmentProcess f = newFragment();
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_QUERY_BY_PAGE", "true");
        Object result = f.runFragment(hints, Collections.emptyMap(), "@@select SELECT * FROM users");
        assertTrue(result instanceof PageQuery);
    }

    @Test
    public void explicitTypeOverridesSql() {
        ExposedExecuteFragmentProcess f = new ExposedExecuteFragmentProcess(newQueryContext());
        SqlConfig config = f.config(new HintsSet(), "@@execute SELECT * FROM users");
        assertEquals(QueryType.Execute, config.getType());
    }

    // --- helpers ---

    private static class ExposedExecuteFragmentProcess extends ExecuteFragmentProcess {
        private ExposedExecuteFragmentProcess(ExecuteContext context) {
            super(context);
        }

        private SqlConfig config(HintsSet hints, String fragmentString) {
            return buildConfig(fragmentString, resolveHints(hints));
        }
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, Object> mapOf(String k, Object v) {
        Map<String, Object> m = new HashMap<>();
        m.put(k, v);
        return m;
    }
}
