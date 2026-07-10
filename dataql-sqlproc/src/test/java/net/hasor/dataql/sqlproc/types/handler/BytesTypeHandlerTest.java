package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.bytes.BytesAsBytesWrapTypeHandler;
import net.hasor.dataql.sqlproc.types.bytes.BytesTypeHandler;
import org.junit.Test;

public class BytesTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testBytesTypeHandler_CallableStatement() throws Throwable {
        BytesTypeHandler handler = new BytesTypeHandler();
        Map<String, Object> values = new HashMap<>();
        byte[] val = new byte[] { 1, 2, 3 };
        values.put("getBytes", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Arrays.equals(val, (byte[]) result);
    }

    @Test
    public void testBytesAsBytesWrapTypeHandler_CallableStatement() throws Throwable {
        BytesAsBytesWrapTypeHandler handler = new BytesAsBytesWrapTypeHandler();
        Map<String, Object> values = new HashMap<>();
        byte[] val = new byte[] { 1, 2, 3 }; // cs returns primitive
        values.put("getBytes", val);

        CallableStatement cs = mockCallableStatement(values);
        Byte[] result = (Byte[]) handler.getResult(cs, 1);
        assert result.length == 3;
        assert result[0] == 1;
        assert result[1] == 2;
        assert result[2] == 3;
    }

    @Test
    public void testBytes() throws Throwable {
        byte[] val = new byte[] { 1, 2, 3, 4 };
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_varbinary) values (?)")) {
            new BytesTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_varbinary from tb_h2_types where c_varbinary is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new BytesTypeHandler().getResult(rs, 1);
                assert res instanceof byte[];
                assert Arrays.equals(val, (byte[]) res);
            }
        }
    }

    @Test
    public void testBytesAsBytesWrap() throws Throwable {
        Byte[] val = new Byte[] { 1, 2, 3, 4 };
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_varbinary) values (?)")) {
            new BytesAsBytesWrapTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_varbinary from tb_h2_types where c_varbinary is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Byte[] res = (Byte[]) new BytesAsBytesWrapTypeHandler().getResult(rs, 1);
                assert Arrays.equals(val, res);
            }
        }
    }
}
