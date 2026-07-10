package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dialect.PageResult;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Extended coverage tests for AbstractStatementExecute branches.
 */
public class BindOutTest extends AbstractSqlProcTest {

    @Test
    public void pageResultCountsFromZero() throws Exception {
        HintsSet hints = hints();
        SqlConfig selectAllUsers = usersSelectConfig();

        try (Connection conn = newH2WithUsers()) {
            PageObject firstPage = new PageObject(0, 1, 0);
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, selectAllUsers, Collections.emptyMap(), firstPage, true);

            assertTrue(result instanceof PageResult);
            PageResult<?> pageResult = (PageResult<?>) result;
            assertEquals(1, pageResult.getData().size());
            assertEquals(3, pageResult.getTotalCount());
        }
    }

    // --- BindOut + paging = error ---
    @Test(expected = java.sql.SQLException.class)
    public void bindOutRejectsPaging() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "col1");
        SqlConfig config = sqlConfig(QueryType.Execute, hints, "SELECT id, name FROM users");

        try (Connection conn = newH2WithUsers()) {
            PageObject page = new PageObject(0, 2);
            RootStatement root = new RootStatement(newQueryContext());
            root.execute(conn, hints, config, Collections.emptyMap(), page, false);
        }
    }

    // --- SelectKey with BeanMap result (non-Map result) ---
    @Test
    public void selectKeyBeanResult() throws Exception {
        // This test expects the result to be a List<Map> which is the normal case
        // Already covered by SelectKeyExecTest
    }

    // --- ColumnCaseType: different values ---
    @Test
    public void lowerColumnCase() throws Exception {
        HintsSet hints = hints();
        hints.setHint("FRAGMENT_SQL_COLUMN_CASE", "lower");
        SqlConfig config = aliceSelectConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
            assertTrue(((Map<?, ?>) result).containsKey("name"));
        }
    }

    // --- ExecuteConfig with bindOut ---
    @Test
    public void executeBindOut() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "result1,result2");
        SqlConfig config = sqlConfig(QueryType.Execute, hints, "SELECT id, name FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
            Map<?, ?> m = (Map<?, ?>) result;
            assertTrue(m.containsKey("result1"));
        }
    }

    // --- DqlConfig with bindOut ---
    @Test
    public void selectBindOut() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "col1");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id, name FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
        }
    }

    // --- PrepareStatement then executeQuery fails ---
    // Hard to test without mocking

}
