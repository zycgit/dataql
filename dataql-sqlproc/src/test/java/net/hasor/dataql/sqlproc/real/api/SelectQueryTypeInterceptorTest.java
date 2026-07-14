package net.hasor.dataql.sqlproc.real.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.execute.TestSqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlInfo;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectQueryTypeInterceptorTest extends AbstractSqlProcTest {
    private String jdbcUrl;

    @Before
    public void setupUsers() throws Exception {
        this.jdbcUrl = "jdbc:h2:mem:interceptor_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        try (Connection conn = rawConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), age INT)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Alice', 25)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Bob', 30)");
        }
    }

    @After
    public void clearInterceptor() {
        TestSqlExecutionInterceptor.clear();
    }

    @Test
    public void dataqlFragmentObservesFinalSqlAndHints() throws Exception {
        AtomicReference<SqlInfo> observed = new AtomicReference<>();
        TestSqlExecutionInterceptor.use(invocation -> {
            observed.set(invocation.getSqlInfo());
            return invocation.proceed();
        });
        Query query = dataQL(context -> context.addInterceptor(new TestSqlExecutionInterceptor())).createQuery("""
                hint FRAGMENT_SQL_DATA_SOURCE = "auditDs"
                var loadUser = @@selectSql(age)<%
                    SELECT name FROM users WHERE age = :age
                %>;
                return loadUser(${age});
                """);

        Object result = unwrap(query.execute(Map.of("age", 25)));

        assertEquals("Alice", result);
        assertEquals("auditDs", observed.get().sourceName());
        assertEquals("auditDs", observed.get().hints().getHint(SqlHintNames.FRAGMENT_SQL_DATA_SOURCE.name()));
        assertEquals("SELECT name FROM users WHERE age = ?", normalizeSql(observed.get().queryString()));
    }

    @Test
    public void dataqlFragmentCanReturnCustomResultWithoutJdbcExecution() throws Exception {
        TestSqlExecutionInterceptor.use(invocation -> Map.of("NAME", "intercepted"));
        Query query = dataQL(context -> context.addInterceptor(new TestSqlExecutionInterceptor())).createQuery("""
                var loadUser = @@selectSql()<%
                    SELECT * FROM table_that_does_not_exist
                %>;
                var user = loadUser();
                return user.NAME;
                """);

        Object result = unwrap(query.execute());

        assertEquals("intercepted", result);
    }

    @Test
    public void dataqlFragmentFiltersInterceptorByFragmentStringAndHints() throws Exception {
        AtomicReference<SqlInfo> observed = new AtomicReference<>();
        TestSqlExecutionInterceptor.use(invocation -> {
            observed.set(invocation.getSqlInfo());
            return invocation.proceed();
        });
        Query query = dataQL(context -> {
            context.addInterceptor(new TestSqlExecutionInterceptor(), (type, fragmentString, hints) -> {
                return type == QueryType.Select && fragmentString.contains("FROM users") && Boolean.TRUE.equals(hints.getHint("enabled"));
            });
        }).createQuery("""
                hint enabled = true
                var loadUser = @@selectSql(name)<%
                    SELECT name FROM users WHERE name = :name
                %>;
                return loadUser(${name});
                """);

        Object result = unwrap(query.execute(Map.of("name", "Bob")));

        assertEquals("Bob", result);
        assertTrue(observed.get().queryString().contains("FROM users"));
    }

    private QueryBuilder dataQL(Consumer<ExecuteContext> customizer) {
        HostConfiguration configuration = new HostConfiguration();
        configuration.addAttachment(ConnectionProvider.class, (sourceName, hints) -> rawConnection());
        ExecuteContext queryContext = configuration.getAttachment(ExecuteContext.class);
        customizer.accept(queryContext);
        return new QueryManager(configuration.getHostContext()).newBuilder();
    }

    private Connection rawConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
    }

    private Object unwrap(QueryResult queryResult) {
        DataModel data = queryResult.getData();
        return data.unwrap();
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
