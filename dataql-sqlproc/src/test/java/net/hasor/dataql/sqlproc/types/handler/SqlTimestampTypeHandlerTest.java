package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampTypeHandler;
import org.junit.Test;

public class SqlTimestampTypeHandlerTest extends AbstractHandlerTest {
    @Test
    public void testSqlTimestampTypeHandler_CallableStatement() throws Throwable {
        SqlTimestampTypeHandler handler = new SqlTimestampTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Timestamp val = new Timestamp(System.currentTimeMillis());
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testTimestamp() throws Throwable {
        Timestamp val = new Timestamp(System.currentTimeMillis());
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new SqlTimestampTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlTimestampTypeHandler().getResult(rs, 1);
                assert res instanceof Timestamp;
                assert Math.abs(val.getTime() - ((Timestamp) res).getTime()) < 1000;
            }
        }
    }
}
