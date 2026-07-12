package net.hasor.dataql.sqlproc.execute.fragment;

import java.util.Collections;
import java.util.Map;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InsertQueryTypeFragmentTest extends AbstractFragmentProcessTest {
    @Test
    public void queryTypeIsInsert() {
        assertEquals(QueryType.Insert, exposed().type("SELECT * FROM users"));
    }

    @Test
    public void plainSqlInsertsRow() throws Throwable {
        Object result = fragment().runFragment(hints(), Collections.emptyMap(), "INSERT INTO users (name, age) VALUES ('N1', 11)");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void paramsInsertRow() throws Throwable {
        Object result = fragment().runFragment(hints(), mapOf("name", "ParamUser", "age", 19), //
                "INSERT INTO users (name, age) VALUES (:name, :age)");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void xmlSqlUsesDynamicValues() throws Throwable {
        Object result = fragment().runFragment(xmlType("insert"), mapOf("name", "XmlUser", "age", 18), //
                "INSERT INTO users (name, age) VALUES (:name,\n" + //
                        "<if test=\"age != null\">:age</if>\n" + //
                        "<if test=\"age == null\">0</if>\n" + //
                        ")");

        assertEquals(1, ((Integer) result).intValue());
    }

    @Test
    public void statementAndTimeoutHintsBuildConfig() {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_STATEMENT.getShortName(), StatementType.Statement.getValue());
        hints.setHint(SqlHintNames.FRAGMENT_SQL_TIMEOUT.getShortName(), "5");

        SqlConfig config = exposed().config(hints, "INSERT INTO users (name, age) VALUES ('S1', 1)");

        assertEquals(StatementType.Statement, config.getStatementType());
        assertEquals(5, config.getTimeout());
    }

    @Test
    public void generatedKeyHintsBuildConfig() {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_KEY_GENERATED.name(), "true");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_KEY_PROPERTY.name(), "id");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_KEY_COLUMN.name(), "ID");

        InsertConfig config = (InsertConfig) exposed().config(hints, "INSERT INTO users (name, age) VALUES ('K1', 1)");

        assertTrue(config.isUseGeneratedKeys());
        assertEquals("id", config.getKeyProperty());
        assertEquals("ID", config.getKeyColumn());
    }

    @Test
    public void selectKeyBeforeWritesBack() throws Throwable {
        InsertFragmentProcess fragment = fragment();
        HintsSet hints = xmlType("insert");
        Map<String, Object> params = mapOf("seed", 1);

        Object result = fragment.runFragment(hints, params, //
                "INSERT INTO users (name, age) VALUES ('BeforeKey', 20)" + //
                        "<selectKey keyProperty=\"keyId\" order=\"before\">SELECT MAX(id) FROM users</selectKey>");

        assertEquals(1, ((Integer) result).intValue());
        assertNotNull(params.get("keyId"));
    }

    @Test
    public void selectKeyAfterWithColumnMappingWritesBack() throws Throwable {
        HintsSet hints = xmlType("insert");
        Map<String, Object> params = mapOf("seed", 1);

        Object result = fragment().runFragment(hints, params, //
                "INSERT INTO users (name, age) VALUES ('AfterKey', 20)" + //
                        "<selectKey keyProperty=\"keyId,count\" keyColumn=\"MAXID,CNT\" order=\"after\">" + //
                        "SELECT MAX(id) AS maxId, COUNT(*) AS cnt FROM users" + //
                        "</selectKey>");

        assertEquals(1, ((Integer) result).intValue());
        assertNotNull(params.get("keyId"));
        assertNotNull(params.get("count"));
    }

    @Test
    public void selectKeyXmlOrderBuildsConfig() {
        HintsSet hints = xmlType("insert");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_ORDER.name(), "before");

        InsertConfig config = (InsertConfig) exposed().config(hints, //
                "INSERT INTO users (name, age) VALUES ('HintOrder', 20)" + //
                        "<selectKey keyProperty=\"keyId\">SELECT MAX(id) FROM users</selectKey>");

        SelectKeyConfig selectKey = config.getSelectKey();
        assertNotNull(selectKey);
        assertEquals("keyId", selectKey.getKeyProperty());
        assertEquals("after", selectKey.getOrder());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fragmentTypeWithoutFormatSuffixFails() throws Throwable {
        fragment().runFragment(fragmentType("insert"), Collections.emptyMap(), "INSERT INTO users (name, age) VALUES ('X', 1)");
    }

    private InsertFragmentProcess fragment() {
        return new InsertFragmentProcess(newQueryContext(users()));
    }

    private ExposedInsertFragmentProcess exposed() {
        return new ExposedInsertFragmentProcess(newQueryContext());
    }

    private class ExposedInsertFragmentProcess extends InsertFragmentProcess {
        private ExposedInsertFragmentProcess(ExecuteContext context) {
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
