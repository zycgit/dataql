package net.hasor.dataql.sqlproc.execute.fragment;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import net.hasor.cobble.function.EFunction;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CallQueryTypeFragmentTest extends AbstractFragmentProcessTest {
    @Test
    public void queryTypeIsCall() {
        assertEquals(QueryType.Call, exposed().type("SELECT * FROM users"));
    }

    @Test
    public void callSqlRunsWithCallableStatement() throws Throwable {
        Object result = fragment().runFragment(hints(), Collections.emptyMap(), "CALL SQLPROC_USER_COUNT()");

        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    public void callXmlRunsWithCallableStatement() throws Throwable {
        Object result = fragment().runFragment(xmlType("call"), Collections.emptyMap(), "CALL SQLPROC_USER_COUNT()");

        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    public void callableStatementOverridesLongAndShortStatementHints() {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_STATEMENT.name(), StatementType.Prepared.getValue());
        hints.setHint(SqlHintNames.FRAGMENT_SQL_STATEMENT.getShortName(), StatementType.Statement.getValue());

        SqlConfig config = exposed().config(hints, "CALL SQLPROC_USER_COUNT()");

        assertEquals(QueryType.Execute, config.getType());
        assertEquals(StatementType.Callable, config.getStatementType());
    }

    @Test
    public void timeoutAndBindOutHintsBuildConfig() {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_TIMEOUT.name(), "8");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_BIND_OUT.getShortName(), "result");

        SqlConfig config = exposed().config(hints, "CALL SQLPROC_USER_COUNT()");

        assertEquals(8, config.getTimeout());
        assertEquals(StatementType.Callable, config.getStatementType());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fragmentTypeWithoutFormatSuffixFails() throws Throwable {
        fragment().runFragment(fragmentType("call"), Collections.emptyMap(), "CALL SQLPROC_USER_COUNT()");
    }

    public static int userCount() {
        return 3;
    }

    private CallFragmentProcess fragment() {
        return new CallFragmentProcess(newQueryContext(procedureDb()));
    }

    private EFunction<String, Connection, SQLException> procedureDb() {
        return name -> {
            Connection conn = newH2WithUsers();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE ALIAS IF NOT EXISTS SQLPROC_USER_COUNT FOR \"" + CallQueryTypeFragmentTest.class.getName() + ".userCount\"");
            }
            return storedProcedureCapable(conn);
        };
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

    private ExposedCallFragmentProcess exposed() {
        return new ExposedCallFragmentProcess(newQueryContext());
    }

    private class ExposedCallFragmentProcess extends CallFragmentProcess {
        public ExposedCallFragmentProcess(ExecuteContext context) {
            super(context);
        }

        private QueryType type(String fragmentString) {
            return queryType(fragmentString, hints());
        }

        private SqlConfig config(HintsSet hints, String fragmentString) {
            return buildConfig(fragmentString, resolveHints(hints));
        }
    }
}
