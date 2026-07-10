package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.OpenPackageType;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dialect.PageResult;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests targeting remaining coverage gaps.
 */
public class ExecuteOptionTest extends AbstractSqlProcTest {

    // --- AbstractStatementExecute: paging with count query ---
    @Test
    public void pageResultCounts() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = aliceSelectConfig();
        PageObject page = new PageObject(0, 2);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), page, true);

            assertTrue(result instanceof PageResult);
        }
    }

    // --- AbstractStatementExecute: paging without pageResult ---
    @Test
    public void pageOffReturnsList() throws Exception {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE.name(), OpenPackageType.Off.getTypeCode());
        SqlConfig config = aliceSelectConfig();
        PageObject page = new PageObject(0, 2);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), page, false);

            assertTrue(result instanceof List);
        }
    }

    // --- AbstractStatementExecute: with timeout hint ---
    @Test
    public void timeoutHint() throws Exception {
        HintsSet hints = hints();
        hints.setHint("timeout", "10");
        SqlConfig config = aliceSelectConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertNotNull(result);
        }
    }

    // --- AbstractStatementExecute: MergedMap data ---
    @Test
    public void mergedMapData() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = aliceSelectConfig();
        MergedMap<String, Object> data = new MergedMap<>();
        data.put("extra", "value");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, data, null, false);

            assertNotNull(result);
        }
    }

    // --- AbstractStatementExecute: non-DQL fetchResult (DML update count) ---
    @Test
    public void dmlUpdateCount() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = updateAliceConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(1, ((Integer) result).intValue());
        }
    }

    // --- AbstractStatementExecute: DQL with fetchSize ---
    @Test
    public void fetchSizeHint() throws Exception {
        HintsSet hints = hints();
        hints.setHint("fetchSize", "100");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id, name FROM users WHERE age > 20");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
            assertEquals(3, ((List<?>) result).size());
        }
    }

    // --- AbstractStatementExecute: DQL with ResultSetType ---
    @Test
    public void scrollSensitive() throws Exception {
        HintsSet hints = hints();
        hints.setHint("resultSetType", "scroll_sensitive");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    // --- AbstractStatementExecute: DQL with scroll_insensitive ---
    @Test
    public void scrollInsensitive() throws Exception {
        HintsSet hints = hints();
        hints.setHint("resultSetType", "scroll_insensitive");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    // --- AbstractStatementExecute: StatementExecute with DQL ---
    @Test
    public void statementSelect() throws Exception {
        HintsSet hints = hints();
        hints.setHint("statementType", "statement");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    // --- Row counting path with totalCount already set ---
    @Test
    public void presetTotalCount() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = aliceSelectConfig();
        PageObject page = new PageObject(0, 2, 100);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), page, true);

            assertTrue(result instanceof PageResult);
            PageResult<?> pr = (PageResult<?>) result;
            assertEquals(100, pr.getTotalCount());
        }
    }

}
