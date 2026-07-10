package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StatementExecTest extends AbstractSqlProcTest {

    @Test
    public void select() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = usersSelectConfig(StatementType.Statement);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
            assertEquals(3, ((List<?>) result).size());
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
    public void delete() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = deleteNoUserConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(0, ((Integer) result).intValue());
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void pagingRejected() throws SQLException {
        HintsSet hints = hints();
        SqlConfig config = usersSelectConfig(StatementType.Statement);
        PageObject page = new PageObject(0, 10);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            root.execute(conn, hints, config, Collections.emptyMap(), page, false);
        }
    }

}
