package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;
import org.junit.Test;

import java.sql.Connection;
import java.util.*;

import static org.junit.Assert.*;

public class RootStatementTest extends AbstractExecuteTest {

    @Test
    public void testDispatchStatement() throws Exception {
        try (Connection conn = newConnection()) {
            RootStatement root = new RootStatement(newQueryContext());
            SqlConfig config = newSelectConfig(StatementType.Statement);
            Object result = root.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
        }
    }

    @Test
    public void testDispatchPrepared() throws Exception {
        try (Connection conn = newConnection()) {
            RootStatement root = new RootStatement(newQueryContext());
            SqlConfig config = newPreparedSelectConfig();
            Object result = root.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertTrue(result instanceof List);
        }
    }

    @Test
    public void testInsertWithoutSelectKey() throws Exception {
        try (Connection conn = newConnection()) {
            RootStatement root = new RootStatement(newQueryContext());
            SqlConfig config = newInsertConfig();
            Object result = root.execute(conn, new HintsSet(), config, Collections.emptyMap(), null, false);
            assertEquals(1, ((Integer) result).intValue());
        }
    }

    @Test
    public void testInsertWithSelectKey() throws Exception {
        try (Connection conn = newConnection()) {
            InsertConfig insertCfg = (InsertConfig) newInsertConfig();

            ArrayDynamicSql skAds = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT MAX(id) FROM users");
                }
            };
            HintsSet skHints = new HintsSet();
            skHints.setHint("keyProperty", "keyId");
            skHints.setHint("keyColumn", "MAX(ID)");
            skHints.setHint("order", "AFTER");
            SelectKeyConfig skConfig = new SelectKeyConfig(skAds, skHints);
            insertCfg.setSelectKey(skConfig);

            RootStatement root = new RootStatement(newQueryContext());
            Map<String, Object> data = new HashMap<>();
            Object result = root.execute(conn, new HintsSet(), insertCfg, data, null, false);

            assertEquals(1, ((Integer) result).intValue());
            assertNotNull(data.get("keyId"));
        }
    }
}
