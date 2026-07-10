package net.hasor.dataql.sqlproc.execute.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.hasor.cobble.function.EFunction;
import net.hasor.dataql.HintNames;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.SqlHintNames;

abstract class AbstractFragmentProcessTest extends AbstractSqlProcTest {
    protected EFunction<String, Connection, SQLException> users() {
        return name -> newH2WithUsers();
    }

    protected HintsSet fragmentType(String fragmentType) {
        HintsSet hints = hints();
        hints.setHint(HintNames.FRAGMENT_TYPE.name(), fragmentType);
        return hints;
    }

    protected HintsSet sqlType(String fragmentType) {
        return fragmentType(fragmentType + "Sql");
    }

    protected HintsSet xmlType(String fragmentType) {
        return fragmentType(fragmentType + "Xml");
    }

    protected HintsSet sqlHint(SqlHintNames hintName, String value) {
        HintsSet hints = hints();
        hints.setHint(hintName.name(), value);
        return hints;
    }

    protected HintsSet shortSqlHint(SqlHintNames hintName, String value) {
        HintsSet hints = hints();
        hints.setHint(hintName.getShortName(), value);
        return hints;
    }

    protected Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    protected Map<String, Object> mapOf(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> map = mapOf(key1, value1);
        map.put(key2, value2);
        return map;
    }

    protected Map<String, Object> mapOf(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        Map<String, Object> map = mapOf(key1, value1, key2, value2);
        map.put(key3, value3);
        return map;
    }
}
