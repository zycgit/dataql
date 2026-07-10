package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.number.NumberTypeHandler;
import org.junit.Test;

public class NumberTypeHandlerTest extends TypeHandlerMockSupport {

    @Test(expected = java.sql.SQLException.class)
    public void testSetParameter() throws Throwable {
        NumberTypeHandler handler = new NumberTypeHandler();
        handler.setParameter(null, 1, 123, null);
    }

    @Test
    public void testNumberTypeHandler_CallableStatement() throws Throwable {
        NumberTypeHandler handler = new NumberTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Integer val = 123;
        values.put("default", val); // NumberTypeHandler calls getObject(index) or getObject(name)

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }
}
