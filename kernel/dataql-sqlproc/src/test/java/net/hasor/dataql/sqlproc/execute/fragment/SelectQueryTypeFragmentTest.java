package net.hasor.dataql.sqlproc.execute.fragment;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.cobble.function.EFunction;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectQueryTypeFragmentTest extends AbstractFragmentProcessTest {
    @Test
    public void queryTypeIsSelect() {
        assertEquals(QueryType.Select, exposed().type("UPDATE users SET age = 1"));
    }

    @Test
    public void plainSqlReturnsRows() throws Throwable {
        Object result = fragment().runFragment(hints(), Collections.emptyMap(), "SELECT * FROM users");

        assertTrue(result instanceof List);
        assertEquals(3, ((List<?>) result).size());
    }

    @Test
    public void paramsReturnSingleRow() throws Throwable {
        Object result = fragment().runFragment(hints(), mapOf("name", "Alice"), "SELECT * FROM users WHERE name = :name");

        assertTrue(result instanceof Map);
        assertEquals("Alice", ((Map<?, ?>) result).get("NAME"));
    }

    @Test
    public void xmlSqlUsesDynamicWhere() throws Throwable {
        Object result = fragment().runFragment(xmlType("select"), mapOf("name", "Alice"), //
                "SELECT * FROM users\n" + //
                        "<where>\n" + //
                        "  <if test=\"name != null\">name = :name</if>\n" + //
                        "</where>");

        assertTrue(result instanceof Map);
        assertEquals("Alice", ((Map<?, ?>) result).get("NAME"));
    }

    @Test
    public void statementHintRunsWithStatement() throws Throwable {
        HintsSet hints = sqlHint(SqlHintNames.FRAGMENT_SQL_STATEMENT, StatementType.Statement.getValue());

        Object result = fragment().runFragment(hints, Collections.emptyMap(), "SELECT id, name, age FROM users");

        assertTrue(result instanceof List);
        assertEquals(3, ((List<?>) result).size());
    }

    @Test
    public void timeoutFetchSizeResultSetTypeBuildConfig() {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_TIMEOUT.name(), "3");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_FETCH_SIZE.getShortName(), "10");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_RESULT_SET_TYPE.getShortName(), "scrollInsensitive");

        SqlConfig config = exposed().config(hints, "SELECT * FROM users");

        assertEquals(3, config.getTimeout());
        DqlConfig dqlConfig = (DqlConfig) config;
        assertEquals(10, dqlConfig.getFetchSize());
        assertEquals(ResultSetType.SCROLL_INSENSITIVE, dqlConfig.getResultSetType());
    }

    @Test
    public void openPackageColumnReturnsSingleColumnValue() throws Throwable {
        HintsSet hints = sqlHint(SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE, "column");

        Object result = fragment().runFragment(hints, Collections.emptyMap(), "SELECT name FROM users WHERE name = 'Alice'");

        assertEquals("Alice", result);
    }

    @Test
    public void openPackageRowReturnsMapForEmptyRows() throws Throwable {
        HintsSet hints = sqlHint(SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE, "row");

        Object result = fragment().runFragment(hints, Collections.emptyMap(), "SELECT * FROM users WHERE 1 = 0");

        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @Test
    public void openPackageOffReturnsList() throws Throwable {
        HintsSet hints = sqlHint(SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE, "off");

        Object result = fragment().runFragment(hints, Collections.emptyMap(), "SELECT * FROM users WHERE name = 'Alice'");

        assertTrue(result instanceof List);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    public void columnCaseLowerUpperAndHump() throws Throwable {
        HintsSet lower = sqlHint(SqlHintNames.FRAGMENT_SQL_COLUMN_CASE, "lower");
        Object lowerResult = fragment().runFragment(lower, Collections.emptyMap(), "SELECT id AS user_id, name FROM users WHERE name = 'Alice'");
        assertTrue(((Map<?, ?>) lowerResult).containsKey("name"));

        HintsSet upper = sqlHint(SqlHintNames.FRAGMENT_SQL_COLUMN_CASE, "upper");
        Object upperResult = fragment().runFragment(upper, Collections.emptyMap(), "SELECT id AS user_id, name FROM users WHERE name = 'Alice'");
        assertTrue(((Map<?, ?>) upperResult).containsKey("NAME"));

        HintsSet hump = sqlHint(SqlHintNames.FRAGMENT_SQL_COLUMN_CASE, "hump");
        Object humpResult = fragment().runFragment(hump, Collections.emptyMap(), "SELECT id AS user_id, name FROM users WHERE name = 'Alice'");
        assertTrue(((Map<?, ?>) humpResult).containsKey("userId"));
    }

    @Test
    public void bindOutFiltersResultAndContextFallback() throws Throwable {
        HintsSet hints = shortSqlHint(SqlHintNames.FRAGMENT_SQL_BIND_OUT, "result,contextOnly");
        Map<String, Object> params = mapOf("contextOnly", "fallback");

        Object result = fragment().runFragment(hints, params, "SELECT id, name FROM users WHERE age > 20");

        assertTrue(result instanceof Map);
        Map<?, ?> resultMap = (Map<?, ?>) result;
        assertTrue(resultMap.containsKey("result"));
        assertEquals("fallback", resultMap.get("contextOnly"));
    }

    @Test
    public void pagingReturnsPageQueryWithOffsetAndDialect() throws Throwable {
        HintsSet hints = hints();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE.name(), "true");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET.name(), "1");
        hints.setHint(SqlHintNames.FRAGMENT_SQL_PAGE_DIALECT.name(), "h2");

        Object result = fragment().runFragment(hints, Collections.emptyMap(), "SELECT * FROM users ORDER BY id");

        assertTrue(result instanceof PageQuery);
        PageQuery pageQuery = (PageQuery) result;
        assertTrue(pageQuery.setPageInfo(mapOf("pageSize", 1, "currentPage", 1)));
        assertEquals(1, ((List<?>) pageQuery.data()).size());
        assertEquals(3L, pageQuery.pageInfo().get("totalCount"));
    }

    @Test
    public void dataSourceHintIsPassedToConnectionSupplier() throws Throwable {
        AtomicReference<String> sourceName = new AtomicReference<>();
        EFunction<String, Connection, SQLException> connection = name -> {
            sourceName.set(name);
            return newH2WithUsers();
        };
        HintsSet hints = sqlHint(SqlHintNames.FRAGMENT_SQL_DATA_SOURCE, "reporting");

        Object result = new SelectFragmentProcess(newQueryContext(connection)).runFragment(hints, Collections.emptyMap(), "SELECT * FROM users");

        assertTrue(result instanceof List);
        assertEquals("reporting", sourceName.get());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fragmentTypeWithoutFormatSuffixFails() throws Throwable {
        fragment().runFragment(fragmentType("select"), Collections.emptyMap(), "SELECT * FROM users");
    }

    private SelectFragmentProcess fragment() {
        return new SelectFragmentProcess(newQueryContext(users()));
    }

    private ExposedSelectFragmentProcess exposed() {
        return new ExposedSelectFragmentProcess(newQueryContext());
    }

    private class ExposedSelectFragmentProcess extends SelectFragmentProcess {
        private ExposedSelectFragmentProcess(ExecuteContext context) {
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
