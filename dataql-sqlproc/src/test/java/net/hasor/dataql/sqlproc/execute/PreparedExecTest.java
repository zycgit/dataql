package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PreparedExecTest extends AbstractSqlProcTest {

    @Test
    public void selectOne() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = aliceSelectConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
            Map<?, ?> row = (Map<?, ?>) result;
            assertEquals("Alice", row.get("NAME"));
        }
    }

    @Test
    public void selectList() throws SQLException {
        HintsSet hints = hints();
        hints.setHint("FRAGMENT_SQL_OPEN_PACKAGE", "off");
        SqlConfig config = aliceSelectConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
            assertEquals(1, ((List<?>) result).size());
            Map<?, ?> row = (Map<?, ?>) ((List<?>) result).get(0);
            assertEquals("Alice", row.get("NAME"));
        }
    }

    @Test
    public void selectColumn() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT name FROM users WHERE name = 'Alice'");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals("Alice", result);
        }
    }

    @Test
    public void insert() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = insertUserConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(1, ((Integer) result).intValue());
        }
    }

    @Test
    public void deleteCount() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = deleteNoUserConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(0, ((Integer) result).intValue());
        }
    }

    @Test
    public void timeoutHint() throws SQLException {
        HintsSet hints = hints();
        hints.setHint("timeout", "5");
        SqlConfig config = usersSelectConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertNotNull(result);
        }
    }

    @Test
    public void pageResult() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = aliceSelectConfig();
        PageObject page = new PageObject(0, 10);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), page, true);

            assertTrue(result instanceof net.hasor.dataql.sqlproc.dialect.PageResult);
            net.hasor.dataql.sqlproc.dialect.PageResult<?> pr = (net.hasor.dataql.sqlproc.dialect.PageResult<?>) result;
            assertEquals(1, pr.getData().size());
        }
    }

}
