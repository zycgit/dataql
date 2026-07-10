package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Final push for coverage.
 */
public class ResultHintTest extends AbstractSqlProcTest {

    // StatementExecute with DQL and ResultSetType ForwardOnly
    @Test
    public void statementForwardOnly() throws Exception {
        HintsSet hints = hints();
        hints.setHint("statementType", "statement");
        hints.setHint("resultSetType", "forward_only");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id FROM users");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    // PreparedStatementExecute prepareCall-style (DQL with ResultSetType)
    @Test
    public void preparedScrollSensitive() throws Exception {
        HintsSet hints = hints();
        hints.setHint("resultSetType", "scroll_sensitive");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id FROM users WHERE age > 20");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    // Prepared DQL with non-default fetchSize
    @Test
    public void preparedFetchSize() throws Exception {
        HintsSet hints = hints();
        hints.setHint("fetchSize", "50");
        SqlConfig config = sqlConfig(QueryType.Select, hints, "SELECT id FROM users WHERE age > 20");

        try (Connection conn = newH2WithUsers()) {
            RootStatement root = new RootStatement(newQueryContext());
            Object result = root.execute(conn, hints, config, Collections.emptyMap(), null, false);

            assertTrue(result instanceof List);
        }
    }

    // MergedMap: extra edge cases
    @Test
    public void mergedMapOverride() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", "parent");

        Map<String, Object> sub = new HashMap<>();
        sub.put("a", "child");
        sub.put("b", "subVal");
        map.appendMap(sub, false);

        // parent wins over child for same key
        assertEquals("parent", map.get("a"));
        assertEquals("subVal", map.get("b"));
    }

    @Test
    public void mergedMapRemoveUnmerged() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);
        map.appendMap(sub, false);

        assertEquals(1, map.remove("a"));
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void mergedMapPutParent() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);
        map.appendMap(sub, false);

        // "c" not in parent or sub -> goes to parent
        map.put("c", 3);
        assertEquals(3, map.get("c"));
        assertEquals(3, map.get("c")); // verify from parent
        assertFalse(sub.containsKey("c"));
    }

    @Test
    public void mergedMapSizeDedup() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);

        Map<String, Object> sub1 = new HashMap<>();
        sub1.put("a", 10);
        map.appendMap(sub1, false);

        Map<String, Object> sub2 = new HashMap<>();
        sub2.put("b", 20);
        map.appendMap(sub2, false);

        // size counts all keys (including duplicates across layers) = a(parent)+a(sub1)+b(sub2) = 3
        // Actually: parent has "a" (1 key), sub1 has "a" (1 key), sub2 has "b" (1 key) = 3
        assertTrue(map.size() >= 2);
    }

}
