package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampAsDateTypeHandler;
import org.junit.Test;

public class SqlTimestampAsDateTypeHandlerTest extends TypeHandlerMockSupport {
    @Test
    public void testSqlTimestampAsDateTypeHandler_CallableStatement() throws Throwable {
        SqlTimestampAsDateTypeHandler handler = new SqlTimestampAsDateTypeHandler(); // Uses getTimestamp usually
        Map<String, Object> values = new HashMap<>();
        Timestamp val = new Timestamp(System.currentTimeMillis());
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert result instanceof Date;
        assert val.getTime() == ((Date) result).getTime();
    }

    @Test
    public void testUtilDate() throws Throwable {
        Date val = new Date();
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new SqlTimestampAsDateTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlTimestampAsDateTypeHandler().getResult(rs, 1);
                assert res instanceof Date;
                // Timestamp to Date might have precision issues or milliseconds
                assert Math.abs(val.getTime() - ((Date) res).getTime()) < 1000;
            }
        }
    }
}
