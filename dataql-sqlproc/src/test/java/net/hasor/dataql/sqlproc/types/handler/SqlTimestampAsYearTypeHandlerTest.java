package net.hasor.dataql.sqlproc.types.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampAsYearTypeHandler;
import org.junit.Test;

public class SqlTimestampAsYearTypeHandlerTest extends AbstractHandlerTest {
    @Test
    public void testYear() throws Throwable {
        java.time.Year val = java.time.Year.now();
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new SqlTimestampAsYearTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlTimestampAsYearTypeHandler().getResult(rs, 1);
                assert res instanceof java.time.Year;
                assert val.equals(res);
            }
        }
    }
}
