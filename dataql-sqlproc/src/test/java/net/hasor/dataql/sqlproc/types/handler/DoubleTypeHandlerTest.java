package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.number.DoubleTypeHandler;
import org.junit.Test;

public class DoubleTypeHandlerTest extends AbstractHandlerTest {

    @Test
    public void testDoubleTypeHandler_CallableStatement() throws Throwable {
        DoubleTypeHandler handler = new DoubleTypeHandler();
        Map<String, Object> values = new HashMap<>();
        values.put("getDouble", 123.45678d);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Double.valueOf(123.45678d).equals(result);
    }

    @Test
    public void testDouble() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_double) values (?)")) {
            new DoubleTypeHandler().setParameter(ps, 1, 123.45678, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_double from tb_h2_types where c_double > 123 and c_double < 124 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new DoubleTypeHandler().getResult(rs, 1);
                assert res instanceof Double;
                assert Math.abs(((Double) res) - 123.45678) < 0.00001;
            }
        }
    }
}
