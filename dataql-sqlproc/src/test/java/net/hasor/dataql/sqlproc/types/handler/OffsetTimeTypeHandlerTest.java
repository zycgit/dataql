package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.OffsetTimeTypeHandler;
import org.junit.Test;

public class OffsetTimeTypeHandlerTest extends AbstractSqlProcTest {
    @Test
    public void testOffsetTime() throws Throwable {
        OffsetTimeTypeHandler handler = new OffsetTimeTypeHandler();
        Map<String, Object> values = new HashMap<>();
        OffsetTime val = OffsetTime.now();
        values.put("getObject", val);

        CallableStatement cs = mockCallableStatement(values);
        OffsetTime result = (OffsetTime) handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testOffsetTime_Full() throws Throwable {
        OffsetTimeTypeHandler handler = new OffsetTimeTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        OffsetTime ot = OffsetTime.now();
        handler.setParameter(ps, 1, ot, null);
        assert captured.get(1).equals(ot);

        // getResult
        Map<String, Object> values = new HashMap<>();
        values.put("getObject", ot);
        values.put("default", ot);

        ResultSet rs = mockResultSet(values);
        OffsetTime res = (OffsetTime) handler.getResult(rs, 1);
        assert res.equals(ot);
    }
}
