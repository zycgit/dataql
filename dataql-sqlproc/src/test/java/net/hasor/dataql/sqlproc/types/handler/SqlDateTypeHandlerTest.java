package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.SqlDateTypeHandler;
import org.junit.Test;

public class SqlDateTypeHandlerTest extends AbstractHandlerTest {
    @Test
    public void testSqlDateTypeHandler_CallableStatement() throws Throwable {
        SqlDateTypeHandler handler = new SqlDateTypeHandler();
        Map<String, Object> values = new HashMap<>();
        java.sql.Date val = new java.sql.Date(System.currentTimeMillis());
        values.put("getDate", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.toString().equals(result.toString());
    }

    @Test
    public void testSqlDate() throws Throwable {
        java.sql.Date val = new java.sql.Date(System.currentTimeMillis());
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_date) values (?)")) {
            new SqlDateTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_date from tb_h2_types where c_date is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlDateTypeHandler().getResult(rs, 1);
                assert res instanceof java.sql.Date;
                assert val.toString().equals(res.toString()); // Date comparison loose
            }
        }
    }
}
