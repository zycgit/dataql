/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.real.jsr223;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.host.jsr223.QueryScriptContext;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryTypeDataQLIntegrationTest extends AbstractSqlProcTest {
    private String jdbcUrl;

    @Before
    public void setupRealDatabase() throws Exception {
        this.jdbcUrl = "jdbc:h2:mem:real_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        try (Connection conn = rawConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), age INT)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Alice', 25)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Bob', 30)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Charlie', 35)");
            stmt.execute("CREATE ALIAS IF NOT EXISTS SQLPROC_REAL_USER_COUNT FOR \"" + QueryTypeDataQLIntegrationTest.class.getName() + ".userCount\"");
        }
    }

    @Test
    public void selectAndInsertQueryTypesUseSqlFragments() throws Exception {
        Map<String, Object> result = objectResult(eval("""
                var loadUsers = @@selectSql(minAge)<%
                    SELECT name, age FROM users WHERE age >= :minAge ORDER BY id
                %>;
                var addUser = @@insertSql(name, age)<%
                    INSERT INTO users (name, age) VALUES (:name, :age)
                %>;
                var insertCount = addUser("Frank", 40);
                var rows = loadUsers(${minAge});
                return {
                    "insertCount": insertCount,
                    "names": rows => [ NAME ]
                };
                """, Map.of("minAge", 35)));

        assertNumber(1, result.get("insertCount"));
        assertEquals(List.of("Charlie", "Frank"), result.get("names"));
    }

    @Test
    public void insertUpdateSelectDeleteQueryTypesUseSqlFragments() throws Exception {
        Map<String, Object> result = objectResult(eval("""
                var insertUser = @@insertSql(name, age)<%
                    INSERT INTO users (name, age) VALUES (:name, :age)
                %>;
                var updateUser = @@updateSql(name, age)<%
                    UPDATE users SET age = :age WHERE name = :name
                %>;
                var loadUser = @@selectSql(name)<%
                    SELECT name, age FROM users WHERE name = :name
                %>;
                var deleteUser = @@deleteSql(name)<%
                    DELETE FROM users WHERE name = :name
                %>;
                var insertCount = insertUser(${name}, ${age});
                var updateCount = updateUser(${name}, ${newAge});
                var loaded = loadUser(${name});
                var deleteCount = deleteUser(${name});
                var afterDelete = loadUser(${name});
                return {
                    "insertCount": insertCount,
                    "updateCount": updateCount,
                    "loadedAge": loaded.AGE,
                    "deleteCount": deleteCount,
                    "afterDelete": afterDelete
                };
                """, Map.of("name", "Dora", "age", 18, "newAge", 19)));

        assertNumber(1, result.get("insertCount"));
        assertNumber(1, result.get("updateCount"));
        assertNumber(19, result.get("loadedAge"));
        assertNumber(1, result.get("deleteCount"));
        assertNull(result.get("afterDelete"));
    }

    @Test
    public void selectQueryTypeUsesXmlFragment() throws Exception {
        List<?> result = listResult(eval("""
                var selectUsers = @@selectXml(minAge)<%
                    SELECT id, name, age FROM users
                    <where>
                        <if test="minAge != null">age &gt;= :minAge</if>
                    </where>
                    ORDER BY id
                %>;
                var rows = selectUsers(${minAge});
                return rows => [
                    {
                        "label": NAME + ":" + AGE,
                        "senior": AGE >= 30
                    }
                ];
                """, Map.of("minAge", 30)));

        assertEquals(2, result.size());
        assertEquals(Map.of("label", "Bob:30", "senior", true), result.get(0));
        assertEquals(Map.of("label", "Charlie:35", "senior", true), result.get(1));
    }

    @Test
    public void insertQueryTypeUsesXmlSelectKey() throws Exception {
        Map<String, Object> result = objectResult(eval("""
                var insertUser = @@insertXml(name, age)<%
                    INSERT INTO users (name, age) VALUES (:name, :age)
                    <selectKey keyProperty="newId" order="after">
                        SELECT MAX(id) FROM users
                    </selectKey>
                %>;
                var loadUser = @@selectSql(name)<%
                    SELECT id, name, age FROM users WHERE name = :name
                %>;
                var count = insertUser(${name}, ${age});
                var loaded = loadUser(${name});
                return { "count": count, "loadedId": loaded.ID, "loadedName": loaded.NAME };
                """, Map.of("name", "Eve", "age", 22)));

        assertNumber(1, result.get("count"));
        assertEquals("Eve", result.get("loadedName"));
        assertNotNull(result.get("loadedId"));
    }

    @Test
    public void updateDeleteExecuteAndCallQueryTypesUseXmlFragments() throws Exception {
        Map<String, Object> result = objectResult(eval("""
                var updateUser = @@updateXml(name, age)<%
                    UPDATE users SET age = :age
                    <where>
                        name = :name
                    </where>
                %>;
                var deleteUser = @@deleteXml(name)<%
                    DELETE FROM users
                    <where>
                        name = :name
                    </where>
                %>;
                var touchUser = @@executeXml(name)<%
                    UPDATE users SET age = age
                    <where>
                        name = :name
                    </where>
                %>;
                var userCount = @@callXml()<%
                    CALL SQLPROC_REAL_USER_COUNT()
                %>;
                return {
                    "updateCount": updateUser("Alice", 26),
                    "touchCount": touchUser("Alice"),
                    "deleteCount": deleteUser("Bob"),
                    "callCount": userCount()
                };
                """));

        assertNumber(1, result.get("updateCount"));
        assertNumber(1, result.get("touchCount"));
        assertNumber(1, result.get("deleteCount"));
        assertNumber(3, result.get("callCount"));
    }

    @Test
    public void callQueryTypeExecutesStoredProcedure() throws Exception {
        Map<String, Object> result = objectResult(eval("""
                var userCount = @@callSql()<%
                    CALL SQLPROC_REAL_USER_COUNT()
                %>;
                var count = userCount();
                return { "count": count };
                """));

        assertNumber(3, result.get("count"));
    }

    @Test
    public void executeQueryTypeRunsGenericSql() throws Exception {
        Object result = eval("""
                var touchUser = @@executeSql(name)<%
                    UPDATE users SET age = age WHERE name = :name
                %>;
                return touchUser(${name});
                """, Map.of("name", "Alice")).getData().unwrap();

        assertNumber(1, result);
    }

    @Test
    public void selectQueryTypeDrivesLazyPagingQuery() throws Exception {
        Map<String, Object> result = objectResult(eval("""
                hint FRAGMENT_SQL_QUERY_BY_PAGE = true
                hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1
                var selectUsers = @@selectSql()<%
                    SELECT id, name, age FROM users ORDER BY id
                %>;
                var pageQuery = selectUsers();
                run pageQuery.setPageInfo({ "pageSize": 2, "currentPage": 1 });
                var data = pageQuery.data();
                var info = pageQuery.pageInfo();
                return {
                    "names": data => [ NAME ],
                    "totalCount": info.totalCount,
                    "pageSize": info.pageSize
                };
                """));

        assertEquals(List.of("Alice", "Bob"), result.get("names"));
        assertNumber(3, result.get("totalCount"));
        assertNumber(2, result.get("pageSize"));
    }

    public static int userCount() {
        return 3;
    }

    private QueryResult eval(String dataql) throws Exception {
        return eval(dataql, Map.of());
    }

    private QueryResult eval(String dataql, Map<String, Object> params) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("dataql");
        QueryScriptContext context = (QueryScriptContext) engine.getContext();
        context.addAttachment(ConnectionProvider.class, (name, hints) -> storedProcedureCapable(rawConnection()));
        Bindings bindings = engine.createBindings();
        bindings.putAll(params);
        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return (QueryResult) engine.eval(dataql, context);
    }

    private Connection rawConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
    }

    private Connection storedProcedureCapable(Connection conn) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class }, (proxy, method, args) -> {
            if ("getMetaData".equals(method.getName()) && method.getParameterCount() == 0) {
                DatabaseMetaData metaData = conn.getMetaData();
                return Proxy.newProxyInstance(DatabaseMetaData.class.getClassLoader(), new Class[] { DatabaseMetaData.class }, (metaProxy, metaMethod, metaArgs) -> {
                    if ("supportsStoredProcedures".equals(metaMethod.getName()) && metaMethod.getParameterCount() == 0) {
                        return true;
                    }
                    return metaMethod.invoke(metaData, metaArgs);
                });
            }
            return method.invoke(conn, args);
        });
    }

    private Map<String, Object> objectResult(QueryResult queryResult) {
        DataModel data = queryResult.getData();
        assertTrue(data.isObject());
        return (Map<String, Object>) data.unwrap();
    }

    private List<?> listResult(QueryResult queryResult) {
        DataModel data = queryResult.getData();
        assertTrue(data.isList());
        return (List<?>) data.unwrap();
    }

    private void assertNumber(long expected, Object value) {
        assertEquals(expected, number(value).longValue());
    }

    private Number number(Object value) {
        assertTrue("value should be a number but was " + value, value instanceof Number);
        return (Number) value;
    }
}
