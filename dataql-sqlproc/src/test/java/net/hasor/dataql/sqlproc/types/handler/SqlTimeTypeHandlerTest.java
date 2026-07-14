package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.SqlTimeTypeHandler;
import org.junit.Test;

public class SqlTimeTypeHandlerTest extends TypeHandlerMockSupport {
    @Test
    public void testSqlTimeTypeHandler_CallableStatement() throws Throwable {
        SqlTimeTypeHandler handler = new SqlTimeTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Time val = new Time(System.currentTimeMillis());
        values.put("getTime", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }
}
