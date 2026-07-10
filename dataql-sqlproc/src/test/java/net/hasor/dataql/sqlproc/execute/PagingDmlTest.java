package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static org.junit.Assert.assertTrue;

public class PagingDmlTest extends AbstractSqlProcTest {

    // PreparedStatementExecute with DML (non-DQL)
    @Test
    public void preparedDmlCount() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = updateAliceConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(1, ((Integer) result).intValue());
        }
    }

    // PreparedStatementExecute: DQL with paging, refresh total count
    @Test
    public void refreshTotalCount() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = usersSelectConfig();
        PageObject page = new PageObject(0, 1, -1);
        page.refreshTotalCount();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), page, true);

            assertTrue(result instanceof PageResult);
            assertEquals(3, ((PageResult<?>) result).getTotalCount());
        }
    }

    // StatementExecute with DML config (fetchResult DmlConfig path)
    @Test
    public void statementDmlCount() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = updateAliceConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(1, ((Integer) result).intValue());
        }
    }

    // PreparedStatementExecute: non-query (execute returns false)
    @Test
    public void preparedDeleteCount() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = deleteNoUserConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(0, ((Integer) result).intValue());
        }
    }

    // Paging with non-zero totalCount (no count SQL)
    @Test
    public void presetTotalList() throws Exception {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE.name(), OpenPackageType.Off.getTypeCode());
        SqlConfig config = usersSelectConfig();
        PageObject page = new PageObject(0, 1, 50);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), page, false);

            assertTrue(result instanceof List);
        }
    }

    // ExecuteConfig (non-DqlConfig) with bindOut
    @Test
    public void executeBindOutList() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "usersResult");
        SqlConfig config = sqlConfig(QueryType.Execute, hints, "SELECT id, name FROM users WHERE age > 20");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
        }
    }

    // DqlConfig bindOut with context fallback
    @Test
    public void selectBindOutContext() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "result,fromContext");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id, name FROM users");
        Map<String, Object> data = new HashMap<>();
        data.put("fromContext", "ctxValue");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, data, null, false);

            assertTrue(result instanceof Map);
            assertEquals("ctxValue", ((Map<?, ?>) result).get("fromContext"));
        }
    }

}
