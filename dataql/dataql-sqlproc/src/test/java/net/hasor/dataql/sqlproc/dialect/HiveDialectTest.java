/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect;
import java.sql.*;
import java.util.List;
import java.util.Map;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.provider.HiveDialect;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HiveDialectTest {
    @Test
    public void firstAndFollowingPagesKeepOnlyBusinessBindings() {
        HiveDialect dialect = new HiveDialect();
        BoundSql original = new BoundSql.BoundSqlObj("SELECT id FROM people WHERE age > ? ORDER BY id", new Object[] { 18 });
        BoundSql first = dialect.pageSql(original, 0, 5);
        BoundSql next = dialect.pageSql(original, 5, 5);
        assertEquals(original.getSqlString() + " LIMIT 5", first.getSqlString());
        assertEquals(original.getSqlString() + " LIMIT 5, 5", next.getSqlString());
        assertArrayEquals(new Object[] { 18 }, first.getArgs());
        assertArrayEquals(new Object[] { 18 }, next.getArgs());
        assertFalse(original.getSqlString().contains("LIMIT"));
        SQLUtils.parseSingleStatement(first.getSqlString(), DbType.hive);
        SQLUtils.parseSingleStatement(next.getSqlString(), DbType.hive);
    }

    @Test
    public void zeroLimitAndInvalidBoundsAreExplicit() {
        HiveDialect dialect = new HiveDialect();
        BoundSql original = new BoundSql.BoundSqlObj("SELECT id FROM people", new Object[0]);
        assertEquals("SELECT id FROM people LIMIT 0", dialect.pageSql(original, 0, 0).getSqlString());
        assertEquals("SELECT id FROM people LIMIT 10, 0", dialect.pageSql(original, 10, 0).getSqlString());
        assertThrows(IllegalArgumentException.class, () -> dialect.pageSql(original, -1, 5));
        assertThrows(IllegalArgumentException.class, () -> dialect.pageSql(original, 0, -1));
    }

    @Test
    public void countRetainsTheOriginalQueryAndBindings() {
        HiveDialect dialect = new HiveDialect();
        for (String query : List.of("SELECT DISTINCT age FROM people WHERE age > ?",//
                "SELECT age, COUNT(*) AS total FROM people GROUP BY age HAVING COUNT(*) > ?")) {
            BoundSql original = new BoundSql.BoundSqlObj(query, new Object[] { 1 });
            BoundSql count = dialect.countSql(original);
            assertEquals("SELECT COUNT(*) FROM (" + query + ") as TEMP_T", count.getSqlString());
            assertArrayEquals(original.getArgs(), count.getArgs());
            SQLUtils.parseSingleStatement(count.getSqlString(), DbType.hive);
        }
    }

    @Test
    public void hiveUrlAndExplicitAliasResolveTheDialect() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn("jdbc:hive2://localhost:10000/default");
        when(metadata.getDatabaseProductVersion()).thenReturn("3.1.3");
        assertTrue(SqlDialectRegister.findDialect(connection, new HintsSet(), null) instanceof HiveDialect);
        HintsSet hints = new HintsSet();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_PAGE_DIALECT.name(), "hive");
        assertTrue(SqlDialectRegister.findDialect(null, hints, null) instanceof HiveDialect);
    }

    @Test
    public void countAndPagingExecuteWithH2MysqlCompatibleSyntax() throws Exception {
        String url = "jdbc:h2:mem:hive_dialect;MODE=MySQL";
        try (Connection connection = DriverManager.getConnection(url); Statement setup = connection.createStatement()) {
            setup.execute("CREATE TABLE people(id INT, age INT)");
            setup.execute("INSERT INTO people VALUES (1,10),(2,20),(3,20),(4,30),(5,30),(6,40)");
            BoundSql original = new BoundSql.BoundSqlObj("SELECT DISTINCT age FROM people WHERE age > ? ORDER BY age", new Object[] { 10 });
            HiveDialect dialect = new HiveDialect();
            BoundSql count = dialect.countSql(original);
            try (PreparedStatement statement = connection.prepareStatement(count.getSqlString())) {
                statement.setObject(1, count.getArgs()[0]);
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals(3, rows.getLong(1));
                }
            }

            HostConfiguration host = new HostConfiguration();
            host.addAttachment(ConnectionProvider.class, (name, hints) -> DriverManager.getConnection(url));
            String script = """
                    hint FRAGMENT_SQL_PAGE_DIALECT = 'hive';
                    hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
                    hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
                    hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
                    var find = @@selectSql(age)<%
                        SELECT DISTINCT age FROM people WHERE age > #{age} ORDER BY age
                    %>;
                    var page = find(10);
                    run page.setPageInfo({'currentPage':2, 'pageSize':1});
                    var rows = page.data();
                    return {'rows':rows, 'total':page.pageInfo().totalCount};
                    """;
            Map<?, ?> result = (Map<?, ?>) new QueryManager(host).newBuilder().createQuery(script).execute().getData().unwrap();
            assertEquals(3, ((Number) result.get("total")).intValue());
            List<?> rows = (List<?>) result.get("rows");
            assertEquals(1, rows.size());
            assertEquals(30, ((Number) ((Map<?, ?>) rows.get(0)).get("age")).intValue());
        }
    }
}
