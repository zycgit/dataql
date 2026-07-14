package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.bool.BooleanTypeHandler;
import org.junit.Test;

public class BooleanTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testBooleanTypeHandler_CallableStatement() throws Throwable {
        BooleanTypeHandler handler = new BooleanTypeHandler();
        Map<String, Object> values = new HashMap<>();
        values.put("getBoolean", true);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Boolean.TRUE.equals(result);
    }

    @Test
    public void testBooleanTypeHandler_1() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_boolean) values (true)")) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_boolean from tb_h2_types where c_boolean is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Boolean res = (Boolean) new BooleanTypeHandler().getResult(rs, 1);
                assert res;
            }
        }
    }

    @Test
    public void testBooleanTypeHandler_2() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_boolean) values (true)")) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_boolean from tb_h2_types where c_boolean is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Boolean res = (Boolean) new BooleanTypeHandler().getResult(rs, "c_boolean");
                assert res;
            }
        }
    }

    @Test
    public void testBooleanTypeHandler_3() throws Throwable {
        BooleanTypeHandler handler = new BooleanTypeHandler();

        try (PreparedStatement ps = conn.prepareStatement("select ?")) {
            handler.setParameter(ps, 1, true, JDBCType.BOOLEAN.getVendorTypeNumber());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Boolean val = (Boolean) handler.getResult(rs, 1);
                assert val;
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("select ?")) {
            handler.setParameter(ps, 1, false, JDBCType.BOOLEAN.getVendorTypeNumber());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Boolean val = (Boolean) handler.getResult(rs, 1);
                assert !val;
            }
        }
    }
}
