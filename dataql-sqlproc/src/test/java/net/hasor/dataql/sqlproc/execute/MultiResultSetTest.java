package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dynamic.config.ExecuteConfig;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;
import org.junit.Test;

import java.sql.Connection;
import java.util.*;

import static org.junit.Assert.*;

public class MultiResultSetTest extends AbstractExecuteTest {

    @Test
    public void testExecuteWithBindOut() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users WHERE age > 20; SELECT COUNT(*) FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "userResult,totalCount");
            ExecuteConfig config = new ExecuteConfig(ads, hints);

            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
            Map<?, ?> resultMap = (Map<?, ?>) result;
            assertTrue(resultMap.containsKey("userResult"));
            assertTrue(resultMap.containsKey("totalCount"));
        }
    }

    @Test
    public void testExecuteWithBindOutAndContextFallback() throws Exception {
        try (Connection conn = newConnection()) {
            ArrayDynamicSql ads = new ArrayDynamicSql() {
                public boolean isHaveInjection() { return false; }
                public boolean isHavePlaceholder() { return false; }
                public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, net.hasor.dataql.sqlproc.dynamic.QueryContext ctx, net.hasor.dataql.sqlproc.dynamic.SqlBuilder builder) {
                    builder.appendSql("SELECT id, name FROM users");
                }
            };
            HintsSet hints = new HintsSet();
            hints.setHint("bindOut", "userResult,missingKey");
            ExecuteConfig config = new ExecuteConfig(ads, hints);

            RootStatement root = new RootStatement(newQueryContext());
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("missingKey", "defaultValue");
            Object result = root.execute(conn, hints, config, ctx, null, false);

            assertTrue(result instanceof Map);
            Map<?, ?> resultMap = (Map<?, ?>) result;
            assertEquals("defaultValue", resultMap.get("missingKey"));
        }
    }
}
