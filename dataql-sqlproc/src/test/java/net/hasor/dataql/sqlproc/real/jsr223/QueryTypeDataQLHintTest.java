package net.hasor.dataql.sqlproc.real.jsr223;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryTypeDataQLHintTest extends AbstractSqlProcTest {
    private String jdbcUrl;

    @Before
    public void setupUsers() throws Exception {
        this.jdbcUrl = "jdbc:h2:mem:hint_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        try (Connection conn = rawConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), age INT)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Alice', 25)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Bob', 30)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Charlie', 35)");
        }
    }

    @Test
    public void selectSqlCanUseStatementHint() throws Exception {
        Object result = unwrap(eval("""
                hint statementType = "statement"
                var loadUsers = @@selectSql()<%
                    SELECT id, name FROM users ORDER BY id
                %>;
                return loadUsers() => [ NAME ];
                """));

        assertEquals(List.of("Alice", "Bob", "Charlie"), result);
    }

    @Test
    public void selectSqlCanReturnListWithOpenPackageOff() throws Exception {
        Object result = unwrap(eval("""
                hint FRAGMENT_SQL_OPEN_PACKAGE = "off"
                var loadUser = @@selectSql(name)<%
                    SELECT id, name FROM users WHERE name = :name
                %>;
                return loadUser(${name}) => [ NAME ];
                """, Map.of("name", "Alice")));

        assertEquals(List.of("Alice"), result);
    }

    @Test
    public void selectSqlCanApplyColumnCaseHint() throws Exception {
        Object result = unwrap(eval("""
                hint FRAGMENT_SQL_COLUMN_CASE = "lower"
                var loadUser = @@selectSql(name)<%
                    SELECT id AS user_id, name FROM users WHERE name = :name
                %>;
                var user = loadUser(${name});
                return user.user_id + ":" + user.name;
                """, Map.of("name", "Alice")));

        assertEquals("1:Alice", result);
    }

    @Test
    public void insertXmlSelectKeyRunsThroughDataQLScript() throws Exception {
        Map<String, Object> result = objectResult(eval("""
                var addUser = @@insertXml(name, age)<%
                    INSERT INTO users (name, age) VALUES (:name, :age)
                    <selectKey keyProperty="newId" order="after">
                        SELECT MAX(id) FROM users
                    </selectKey>
                %>;
                var loadUser = @@selectSql(name)<%
                    SELECT id, name, age FROM users WHERE name = :name
                %>;
                var count = addUser(${name}, ${age});
                var user = loadUser(${name});
                return {
                    "count": count,
                    "id": user.ID,
                    "age": user.AGE
                };
                """, Map.of("name", "Dora", "age", 18)));

        assertNumber(1, result.get("count"));
        assertNumber(4, result.get("id"));
        assertNumber(18, result.get("age"));
    }

    private QueryResult eval(String dataql) throws Exception {
        return eval(dataql, Map.of());
    }

    private QueryResult eval(String dataql, Map<String, Object> params) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("dataql");
        QueryScriptContext context = (QueryScriptContext) engine.getContext();
        context.addAttachment(ConnectionProvider.class, (name, hints) -> rawConnection());
        Bindings bindings = engine.createBindings();
        bindings.putAll(params);
        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return (QueryResult) engine.eval(dataql, context);
    }

    private Connection rawConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
    }

    private Object unwrap(QueryResult queryResult) {
        return queryResult.getData().unwrap();
    }

    private Map<String, Object> objectResult(QueryResult queryResult) {
        DataModel data = queryResult.getData();
        assertTrue(data.isObject());
        return (Map<String, Object>) data.unwrap();
    }

    private void assertNumber(long expected, Object value) {
        assertTrue("value should be a number but was " + value, value instanceof Number);
        assertEquals(expected, ((Number) value).longValue());
    }
}
