package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.number.ByteTypeHandler;
import org.junit.Test;

public class ByteTypeHandlerTest extends AbstractSqlProcTest {

    @Test
    public void testByteTypeHandler_CallableStatement() throws Throwable {
        ByteTypeHandler handler = new ByteTypeHandler();
        Map<String, Object> values = new HashMap<>();
        values.put("getByte", (byte) 123);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Byte.valueOf((byte) 123).equals(result);
    }

    @Test
    public void testByte() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_tinyint) values (?)")) {
            new ByteTypeHandler().setParameter(ps, 1, (byte) 123, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_tinyint from tb_h2_types where c_tinyint = 123 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new ByteTypeHandler().getResult(rs, 1);
                assert res instanceof Byte;
                assert ((Byte) res) == 123;

                Object res2 = new ByteTypeHandler().getResult(rs, "c_tinyint");
                assert ((Byte) res2) == 123;
            }
        }
    }
}
