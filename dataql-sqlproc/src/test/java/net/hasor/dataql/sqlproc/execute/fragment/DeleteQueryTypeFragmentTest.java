package net.hasor.dataql.sqlproc.execute.fragment;

import java.util.Collections;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DeleteQueryTypeFragmentTest extends AbstractFragmentProcessTest {
    @Test
    public void queryTypeIsDelete() {
        assertEquals(QueryType.Delete, exposed().type("SELECT * FROM users"));
    }

    @Test
    public void plainSqlDeletesRows() throws Throwable {
        Object result = fragment().runFragment(hints(), Collections.emptyMap(), "DELETE FROM users WHERE name = 'Charlie'");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void paramsDeleteRows() throws Throwable {
        Object result = fragment().runFragment(hints(), mapOf("name", "Bob"), "DELETE FROM users WHERE name = :name");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void xmlSqlUsesDynamicWhere() throws Throwable {
        Object result = fragment().runFragment(xmlType("delete"), mapOf("name", "Alice"), //
                "DELETE FROM users\n" + //
                        "<where>\n" + //
                        "  <if test=\"name != null\">name = :name</if>\n" + //
                        "</where>");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void statementAndTimeoutHintsBuildConfig() {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_STATEMENT.name(), StatementType.Prepared.getValue());
        hints.setHint(SqlHintNames.FRAGMENT_SQL_TIMEOUT.name(), "7");

        SqlConfig config = exposed().config(hints, "DELETE FROM users WHERE name = 'Alice'");

        assertEquals(StatementType.Prepared, config.getStatementType());
        assertEquals(7, config.getTimeout());
    }

    @Test
    public void statementHintRunsWithStatement() throws Throwable {
        HintsSet hints = sqlHint(SqlHintNames.FRAGMENT_SQL_STATEMENT, StatementType.Statement.getValue());

        Object result = fragment().runFragment(hints, Collections.emptyMap(), "DELETE FROM users WHERE id > 100");

        assertEquals(0, ((Integer) result).intValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fragmentTypeWithoutFormatSuffixFails() throws Throwable {
        fragment().runFragment(fragmentType("delete"), Collections.emptyMap(), "DELETE FROM users WHERE id = 1");
    }

    private DeleteFragmentProcess fragment() {
        return new DeleteFragmentProcess(newQueryContext(users()));
    }

    private ExposedDeleteFragmentProcess exposed() {
        return new ExposedDeleteFragmentProcess(newQueryContext());
    }

    private class ExposedDeleteFragmentProcess extends DeleteFragmentProcess {
        private ExposedDeleteFragmentProcess(ExecuteContext context) {
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
