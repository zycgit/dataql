package net.hasor.dataql.sqlproc.types.handler;

import java.lang.reflect.Proxy;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.array.ArrayTypeHandler;
import net.hasor.dataql.sqlproc.types.array.PgArrayTypeHandler;
import org.junit.Test;

public class ArrayTypeHandlerTest extends AbstractSqlProcTest {

    private Array mockArray(Object content) {
        return (Array) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Array.class }, (proxy, method, args) -> {
            if ("getArray".equals(method.getName()))
                return content;
            if ("free".equals(method.getName()))
                return null;
            if ("getResultSet".equals(method.getName())) {
                // Mock ResultSet for PgArrayTypeHandler
                // This is complex. PgArrayTypeHandler expects a ResultSet with "VALUE" column.
                // We might skip mocking ResultSet logic if we rely on ArrayTypeHandler which uses getArray.
                // PgArrayTypeHandler extractArray logic:
                // try (ResultSet rs = array.getResultSet()) { while (rs.next()) { ... readArrayHandler ... } }
                // It relies on array.getResultSet().
                return mockResultSetForArray((Object[]) content);
            }
            return null;
        });
    }

    private ResultSet mockResultSetForArray(Object[] content) {
        // A simple Mock ResultSet that iterates over content
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ResultSet.class }, new java.lang.reflect.InvocationHandler() {
            int index = -1;

            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                if ("next".equals(method.getName())) {
                    index++;
                    return index < content.length;
                }
                if ("close".equals(method.getName()))
                    return null;
                if ("getObject".equals(method.getName()))
                    return content[index];
                if ("getString".equals(method.getName()))
                    return content[index].toString();
                if ("getBytes".equals(method.getName()))
                    return content[index]; // simple cast
                return null;
            }
        });
    }

    @Test
    public void testArrayTypeHandler_CallableStatement() throws Throwable {
        ArrayTypeHandler handler = new ArrayTypeHandler();
        Map<String, Object> values = new HashMap<>();
        String[] val = new String[] { "a", "b" };
        Array array = mockArray(val);
        values.put("getArray", array);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert result instanceof String[];
        assert ((String[]) result).length == 2;
    }

    @Test
    public void testPgArrayTypeHandler_CallableStatement() throws Throwable {
        // default PG handler (not money/bit etc)
        PgArrayTypeHandler handler = new PgArrayTypeHandler("text", 1);
        Map<String, Object> values = new HashMap<>();
        String[] val = new String[] { "a", "b" }; // PgArrayTypeHandler writes to RS, reads from RS
        Array array = mockArray(val);
        values.put("getArray", array);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert result instanceof Object[];
        assert ((Object[]) result).length == 2;
        assert "a".equals(((Object[]) result)[0]);
    }

    @Test
    public void testArray() throws Throwable {
        Object[] val = new Object[] { "a", "b" };
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (a_char) values (?)")) {
            new ArrayTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select a_char from tb_h2_types limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new ArrayTypeHandler().getResult(rs, 1);
                assert res instanceof Object[];
                Object[] arr = (Object[]) res;
                assert arr.length == 2;
                assert "a".equals(arr[0]);
            }
        }
    }
}
