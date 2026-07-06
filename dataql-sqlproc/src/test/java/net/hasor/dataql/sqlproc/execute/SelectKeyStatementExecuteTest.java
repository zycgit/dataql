package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class SelectKeyStatementExecuteTest extends AbstractExecuteTest {

    @Test
    public void testSelectKeyBefore() throws Exception {
        try (Connection conn = newConnection()) {
            InsertConfig insertCfg = (InsertConfig) newInsertConfig();
            insertCfg.setSelectKey(makeSelectKey("SELECT MAX(id) FROM users", "keyId", "MAX(ID)", "BEFORE"));

            RootStatement root = new RootStatement(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            Object result = root.execute(conn, new HintsSet(), insertCfg, data, null, false);

            assertEquals(1, ((Integer) result).intValue());
            assertNotNull(data.get("keyId"));
        }
    }

    @Test
    public void testSelectKeyAfter() throws Exception {
        try (Connection conn = newConnection()) {
            InsertConfig insertCfg = (InsertConfig) newInsertConfig();
            insertCfg.setSelectKey(makeSelectKey("SELECT MAX(id) FROM users", "keyId", "MAX(ID)", "AFTER"));

            RootStatement root = new RootStatement(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            Object result = root.execute(conn, new HintsSet(), insertCfg, data, null, false);

            assertEquals(1, ((Integer) result).intValue());
            assertNotNull(data.get("keyId"));
        }
    }

    @Test
    public void testSelectKeyWithoutKeyColumn() throws Exception {
        try (Connection conn = newConnection()) {
            InsertConfig insertCfg = (InsertConfig) newInsertConfig();
            // No keyColumn specified - value from first column of result
            insertCfg.setSelectKey(makeSelectKey("SELECT 999 AS val", "keyId", null, "AFTER"));

            RootStatement root = new RootStatement(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            root.execute(conn, new HintsSet(), insertCfg, data, null, false);
            assertNotNull(data.get("keyId"));
        }
    }

    @Test
    public void testSelectKeyMultiProperty() throws Exception {
        try (Connection conn = newConnection()) {
            InsertConfig insertCfg = (InsertConfig) newInsertConfig();
            insertCfg.setSelectKey(makeSelectKey("SELECT MAX(id) AS maxId, COUNT(*) AS cnt FROM users", "keyId,count", "MAXID,CNT", "AFTER"));

            RootStatement root = new RootStatement(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            root.execute(conn, new HintsSet(), insertCfg, data, null, false);

            assertNotNull(data.get("keyId"));
            assertNotNull(data.get("count"));
        }
    }

    @Test(expected = SQLException.class)
    public void testSelectKeyMismatchSize() throws Exception {
        try (Connection conn = newConnection()) {
            InsertConfig insertCfg = (InsertConfig) newInsertConfig();
            insertCfg.setSelectKey(makeSelectKey("SELECT MAX(id) FROM users", "keyId,count", "MAX(ID)", "AFTER"));

            RootStatement root = new RootStatement(newQueryContext());
            root.execute(conn, new HintsSet(), insertCfg, new HashMap<>(), null, false);
        }
    }

    private SelectKeyConfig makeSelectKey(String sql, String keyProperty, String keyColumn, String order) {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return false; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                builder.appendSql(sql);
            }
        };
        HintsSet hints = new HintsSet();
        hints.setHint("keyProperty", keyProperty);
        if (keyColumn != null) hints.setHint("keyColumn", keyColumn);
        hints.setHint("order", order);
        return new SelectKeyConfig(ads, hints);
    }
}
