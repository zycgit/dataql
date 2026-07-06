package net.hasor.dataql.sqlproc.types.handler;

import java.sql.PreparedStatement;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import java.sql.ResultSet;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;
import net.hasor.dataql.sqlproc.types.UnknownTypeHandler;
import org.junit.Test;

public class UnknownTypeHandlerTest extends AbstractSqlProcTest {
    @Test
    public void testUnknown() throws Throwable {
        UnknownTypeHandler handler = new UnknownTypeHandler(TypeHandlerRegistry.DEFAULT);

        // Integer (registered)
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_integer) values (?)")) {
            handler.setParameter(ps, 1, 12345, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_integer from tb_h2_types where c_integer = 12345 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                // Should resolve IntegerTypeHandler via metadata/registry
                Object res = handler.getResult(rs, 1);
                assert res instanceof Integer;
                assert ((Integer) res) == 12345;
            }
        }

        // String (registered)
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_varchar) values (?)")) {
            handler.setParameter(ps, 1, "hello", null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_varchar from tb_h2_types where c_varchar = 'hello' limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = handler.getResult(rs, "c_varchar");
                assert "hello".equals(res);
            }
        }
    }
}
