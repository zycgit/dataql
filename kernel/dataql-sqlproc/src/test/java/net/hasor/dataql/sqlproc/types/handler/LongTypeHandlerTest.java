package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.number.LongTypeHandler;
import org.junit.Test;

public class LongTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testLongTypeHandler_CallableStatement() throws Throwable {
        LongTypeHandler handler = new LongTypeHandler();
        Map<String, Object> values = new HashMap<>();
        values.put("getLong", 123456789L);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Long.valueOf(123456789L).equals(result);
    }

    @Test
    public void testLong() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_bigint) values (?)")) {
            new LongTypeHandler().setParameter(ps, 1, 1234567890L, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_bigint from tb_h2_types where c_bigint = 1234567890 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new LongTypeHandler().getResult(rs, 1);
                assert res instanceof Long;
                assert ((Long) res) == 1234567890L;

                Object res2 = new LongTypeHandler().getResult(rs, "c_bigint");
                assert ((Long) res2) == 1234567890L;
            }
        }
    }
}
