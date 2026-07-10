package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.config.InsertConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SelectKeyExecTest extends AbstractSqlProcTest {

    @Test
    public void beforeInsert() throws Exception {
        HintsSet hints = hints();
        InsertConfig insertCfg = insertWithSelectKeyConfig("SELECT MAX(id) FROM users", "keyId", null, "BEFORE");
        Map<String, Object> data = new HashMap<>();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, insertCfg, data, null, false);

            assertEquals(1, ((Integer) result).intValue());
            assertNotNull(data.get("keyId"));
        }
    }

    @Test
    public void afterInsert() throws Exception {
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

    @Test
    public void noKeyColumn() throws Exception {
        HintsSet hints = hints();
        InsertConfig insertCfg = insertWithSelectKeyConfig("SELECT 999 AS val", "keyId", null, "AFTER");
        Map<String, Object> data = new HashMap<>();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            root.execute(conn, hints, insertCfg, data, null, false);

            assertNotNull(data.get("keyId"));
        }
    }

    @Test
    public void multiProperty() throws Exception {
        HintsSet hints = hints();
        InsertConfig insertCfg = insertWithSelectKeyConfig("SELECT MAX(id) AS maxId, COUNT(*) AS cnt FROM users", "keyId,count", "MAXID,CNT", "AFTER");
        Map<String, Object> data = new HashMap<>();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            root.execute(conn, hints, insertCfg, data, null, false);

            assertNotNull(data.get("keyId"));
            assertNotNull(data.get("count"));
        }
    }

    @Test(expected = SQLException.class)
    public void mismatchSize() throws Exception {
        HintsSet hints = hints();
        InsertConfig insertCfg = insertWithSelectKeyConfig("SELECT MAX(id) FROM users", "keyId,count", "MAX(ID)", "AFTER");
        Map<String, Object> data = new HashMap<>();

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            root.execute(conn, hints, insertCfg, data, null, false);
        }
    }

}
