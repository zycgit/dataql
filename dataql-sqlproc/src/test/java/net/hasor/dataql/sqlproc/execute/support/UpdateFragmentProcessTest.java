package net.hasor.dataql.sqlproc.execute.support;

import java.util.Collections;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpdateFragmentProcessTest extends AbstractFragmentProcessTest {
    @Test
    public void queryTypeIsUpdate() {
        assertEquals(QueryType.Update, exposed().type("SELECT * FROM users"));
    }

    @Test
    public void plainSqlUpdatesRows() throws Throwable {
        Object result = fragment().runFragment(hints(), Collections.emptyMap(), "UPDATE users SET age = 12 WHERE name = 'Alice'");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void paramsUpdateRows() throws Throwable {
        Object result = fragment().runFragment(hints(), mapOf("name", "Alice", "age", 33), //
                "UPDATE users SET age = :age WHERE name = :name");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void xmlSqlUsesDynamicValue() throws Throwable {
        Object result = fragment().runFragment(xmlType("update"), mapOf("name", "Alice", "age", 44), //
                "UPDATE users SET age = " + //
                        "<if test=\"age != null\">:age</if>" + //
                        "<if test=\"age == null\">age</if>" + //
                        " WHERE name = :name");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void shortStatementAndTimeoutHintsBuildConfig() {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_STATEMENT.getShortName(), StatementType.Prepared.getValue());
        hints.setHint(SqlHintNames.FRAGMENT_SQL_TIMEOUT.getShortName(), "6");

        SqlConfig config = exposed().config(hints, "UPDATE users SET age = 1 WHERE name = 'Alice'");

        assertEquals(StatementType.Prepared, config.getStatementType());
        assertEquals(6, config.getTimeout());
    }

    @Test
    public void statementHintRunsWithStatement() throws Throwable {
        HintsSet hints = sqlHint(SqlHintNames.FRAGMENT_SQL_STATEMENT, StatementType.Statement.getValue());

        Object result = fragment().runFragment(hints, Collections.emptyMap(), "UPDATE users SET age = 21 WHERE name = 'Bob'");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fragmentTypeWithoutFormatSuffixFails() throws Throwable {
        fragment().runFragment(fragmentType("update"), Collections.emptyMap(), "UPDATE users SET age = 1");
    }

    private UpdateFragmentProcess fragment() {
        return new UpdateFragmentProcess(users(), newQueryContext());
    }

    private ExposedUpdateFragmentProcess exposed() {
        return new ExposedUpdateFragmentProcess(newQueryContext());
    }

    private class ExposedUpdateFragmentProcess extends UpdateFragmentProcess {
        private ExposedUpdateFragmentProcess(QueryContext queryContext) {
            super(name -> null, queryContext);
        }

        private QueryType type(String fragmentString) {
            return queryType(fragmentString, hints());
        }

        private SqlConfig config(HintsSet hints, String fragmentString) {
            return buildConfig(fragmentString, resolveHints(hints));
        }
    }
}
