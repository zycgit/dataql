package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.config.InsertConfig;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RootExecTest extends AbstractSqlProcTest {

    @Test
    public void dispatchStatement() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = usersSelectConfig(StatementType.Statement);

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    @Test
    public void dispatchPrepared() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = aliceSelectConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
        }
    }

    @Test
    public void insertNoSelectKey() throws Exception {
        HintsSet hints = hints();
        SqlConfig config = insertUserConfig();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertEquals(1, ((Integer) result).intValue());
        }
    }

    @Test
    public void insertSelectKey() throws Exception {
        HintsSet hints = hints();
        InsertConfig insertCfg = insertWithSelectKeyConfig("SELECT MAX(id) FROM users", "keyId", null, "AFTER");
        Map<String, Object> data = new HashMap<>();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, insertCfg, data, null, false);

            assertEquals(1, ((Integer) result).intValue());
            assertNotNull(data.get("keyId"));
        }
    }

}
