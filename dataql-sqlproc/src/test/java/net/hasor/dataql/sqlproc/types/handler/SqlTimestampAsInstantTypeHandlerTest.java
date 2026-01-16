package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampAsInstantTypeHandler;
import org.junit.Test;

public class SqlTimestampAsInstantTypeHandlerTest extends AbstractHandlerTest {
    @Test
    public void testSqlTimestampAsInstant() throws Throwable {
        SqlTimestampAsInstantTypeHandler handler = new SqlTimestampAsInstantTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Timestamp val = Timestamp.from(Instant.now());
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        Instant result = (Instant) handler.getResult(cs, 1);
        assert val.toInstant().equals(result);
    }

    @Test
    public void testSqlTimestampAsInstant_Full() throws Throwable {
        SqlTimestampAsInstantTypeHandler handler = new SqlTimestampAsInstantTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        Instant inst = Instant.now();
        handler.setParameter(ps, 1, inst, null);
        assert captured.get(1) instanceof Timestamp;
        assert ((Timestamp) captured.get(1)).toInstant().equals(inst);

        // getResult
        Map<String, Object> values = new HashMap<>();
        Timestamp ts = Timestamp.from(inst);
        values.put("getTimestamp", ts);

        ResultSet rs = mockResultSet(values);
        Instant res = (Instant) handler.getResult(rs, 1);
        assert res.equals(inst);
    }
}
