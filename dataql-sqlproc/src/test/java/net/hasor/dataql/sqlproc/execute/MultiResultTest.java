package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiResultTest extends AbstractSqlProcTest {

    @Test
    public void bindOutMultipleResults() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "userResult,totalCount");
        SqlConfig config = sqlConfig(QueryType.Execute, hints, "SELECT id, name FROM users WHERE age > 20; SELECT COUNT(*) FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof Map);
            Map<?, ?> resultMap = (Map<?, ?>) result;
            assertTrue(resultMap.containsKey("userResult"));
            assertTrue(resultMap.containsKey("totalCount"));
        }
    }

    @Test
    public void bindOutUsesContext() throws Exception {
        HintsSet hints = hints();
        hints.setHint("bindOut", "userResult,missingKey");
        SqlConfig config = sqlConfig(QueryType.Execute, hints, "SELECT id, name FROM users");
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("missingKey", "defaultValue");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, ctx, null, false);

            assertTrue(result instanceof Map);
            Map<?, ?> resultMap = (Map<?, ?>) result;
            assertEquals("defaultValue", resultMap.get("missingKey"));
        }
    }

}
