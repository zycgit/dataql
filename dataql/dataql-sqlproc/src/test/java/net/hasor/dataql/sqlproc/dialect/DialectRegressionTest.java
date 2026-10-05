/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect;
import java.sql.*;
import java.util.Map;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.provider.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DialectRegressionTest {
    @Test
    public void h2PaginationBindsOnlyTheGeneratedPlaceholders() throws Exception {
        BoundSql original = new BoundSql.BoundSqlObj("select x from system_range(1,10) where x > ? order by x", new Object[] { 2 });
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:pagination")) {
            PageDialect dialect = SqlDialectRegister.findDialect(connection, new HintsSet(), null);
            BoundSql page = dialect.pageSql(original, 2, 3);
            try (PreparedStatement statement = connection.prepareStatement(page.getSqlString())) {
                for (int i = 0; i < page.getArgs().length; i++) {
                    statement.setObject(i + 1, page.getArgs()[i]);
                }
                try (ResultSet rows = statement.executeQuery()) {
                    for (int value : new int[] { 5, 6, 7 }) {
                        assertTrue(rows.next());
                        assertEquals(value, rows.getInt(1));
                    }
                    assertFalse(rows.next());
                }
            }
        }
    }

    @Test
    public void db2PaginationUsesInclusiveRowNumberBounds() {
        BoundSql original = new BoundSql.BoundSqlObj("select id from users where age > ?", new Object[] { 18 });
        BoundSql page = new Db2Dialect().pageSql(original, 10, 5);
        assertArrayEquals(new Object[] { 18, 11L, 15L }, page.getArgs());
        assertArrayEquals(new Object[] { 18, 1L, 5L }, new Db2Dialect().pageSql(original, 0, 5).getArgs());
    }

    @Test
    public void sqlServerPagingDoesNotAddPhantomArguments() {
        BoundSql original = new BoundSql.BoundSqlObj("select id from users where age > ? order by id", new Object[] { 18 });
        PageDialect dialect = new SqlServerDialect();
        BoundSql page = dialect.pageSql(original, 10, 5);
        assertTrue(page.getSqlString().contains("BETWEEN 11 AND 15"));
        assertEquals(1, page.getArgs().length);
        assertArrayEquals(original.getArgs(), page.getArgs());
        BoundSql count = dialect.countSql(original);
        assertEquals("SELECT COUNT(*) FROM (select id from users where age > ? ) as TEMP_T", count.getSqlString());
        assertArrayEquals(original.getArgs(), count.getArgs());
    }

    @Test
    public void adapterPaginationPreservesBoundValues() {
        BoundSql mongo = new BoundSql.BoundSqlObj("db.users.find({id: ?})", new Object[] { 7 });
        BoundSql page = new MongoDialect().pageSql(mongo, 10, 5);
        assertEquals("/*+overwrite_find_skip=10,overwrite_find_limit=5*/db.users.find({id: ?})", page.getSqlString());
        assertArrayEquals(mongo.getArgs(), page.getArgs());
        BoundSql elastic = new BoundSql.BoundSqlObj("POST /users/_search {\"query\":{\"term\":{\"id\":?}}}", mongo.getArgs());
        assertTrue(new ElasticDialect().pageSql(elastic, 0, 5).getSqlString().startsWith("/*+overwrite_find_limit=5*/"));
        assertTrue(new ElasticDialect().countSql(elastic).getSqlString().startsWith("/*+overwrite_find_as_count*/"));
        BoundSql milvus = new BoundSql.BoundSqlObj("SELECT * FROM users WHERE id > ?", mongo.getArgs());
        BoundSql milvusPage = new MilvusDialect().pageSql(milvus, 10, 5);
        assertEquals(milvus.getSqlString() + " LIMIT 5 OFFSET 10", milvusPage.getSqlString());
        assertArrayEquals(milvus.getArgs(), milvusPage.getArgs());
        BoundSql clickhouse = new ClickHouseDialect().pageSql(milvus, 10, 5);
        assertArrayEquals(new Object[] { 7, 10L, 5L }, clickhouse.getArgs());
    }

    @Test
    public void mongoAggregationPagesAfterExistingStages() {
        BoundSql original = new BoundSql.BoundSqlObj("db.users.aggregate([{$match: {age: {$gte: ?}}}])", new Object[] { 18 });
        BoundSql page = new MongoDialect().pageSql(original, 10, 5);
        assertEquals("db.users.aggregate([{$match: {age: {$gte: ?}}}, {$skip: 10}, {$limit: 5}])", page.getSqlString());
        assertArrayEquals(original.getArgs(), page.getArgs());
        BoundSql count = new MongoDialect().countSql(original);
        assertTrue(count.getSqlString().contains("$count: 'value'"));
        assertTrue(count.getSqlString().contains("$ifNull"));
        assertArrayEquals(original.getArgs(), count.getArgs());
    }

    @Test
    public void mongoAggregationPreservesWhitespaceNestedArraysAndOptions() {
        MongoDialect dialect = new MongoDialect();
        String command = "\n db.users.aggregate ( [{$match: {id: {$in: [?]}, text: ']'}}], {allowDiskUse: true}); \n";
        BoundSql original = new BoundSql.BoundSqlObj(command, new Object[] { 7 });
        BoundSql page = dialect.pageSql(original, 0, 5);
        assertEquals(command.replace("], {allowDiskUse", ", {$limit: 5}], {allowDiskUse"), page.getSqlString());
        assertArrayEquals(original.getArgs(), page.getArgs());
        BoundSql empty = new BoundSql.BoundSqlObj("db.users.aggregate( [ ] )", new Object[0]);
        assertEquals("db.users.aggregate( [ {$limit: 5}] )", dialect.pageSql(empty, 0, 5).getSqlString());
        BoundSql find = new BoundSql.BoundSqlObj("db.users.find({text: '.aggregate(['})", new Object[0]);
        assertEquals("/*+overwrite_find_limit=5*/" + find.getSqlString(), dialect.pageSql(find, 0, 5).getSqlString());
    }

    @Test
    public void adapterUrlsAndAliasesResolveWithoutOrmDependencies() throws Exception {
        Map<String, Class<?>> adapters = Map.of("mongo", MongoDialect.class, "elastic", ElasticDialect.class, "milvus", MilvusDialect.class);
        for (Map.Entry<String, Class<?>> adapter : adapters.entrySet()) {
            Connection connection = mock(Connection.class);
            DatabaseMetaData metadata = mock(DatabaseMetaData.class);
            when(connection.getMetaData()).thenReturn(metadata);
            when(metadata.getURL()).thenReturn("jdbc:dbvisitor:" + adapter.getKey() + "://localhost/test");
            when(metadata.getDatabaseProductVersion()).thenReturn("8.15.0");
            assertEquals(adapter.getValue(), SqlDialectRegister.findDialect(connection, new HintsSet(), null).getClass());
        }
        assertEquals(JdbcHelper.REDIS, JdbcHelper.getDbType("jdbc:dbvisitor:jedis://localhost", null));
        assertEquals(JdbcHelper.ELASTIC6, JdbcHelper.getDbType("jdbc:dbvisitor:elastic://localhost", null, "6.8.0"));
        assertEquals(JdbcHelper.ELASTIC7, JdbcHelper.getDbType("jdbc:dbvisitor:elastic://localhost", null, "7.17.0"));
        HintsSet hints = new HintsSet();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_PAGE_DIALECT.name(), "clickhouse");
        assertTrue(SqlDialectRegister.findDialect(null, hints, null) instanceof ClickHouseDialect);
        hints.setHint(SqlHintNames.FRAGMENT_SQL_PAGE_DIALECT.name(), "CLICKHOUSE");
        assertTrue(SqlDialectRegister.findDialect(null, hints, null) instanceof ClickHouseDialect);
    }
}
