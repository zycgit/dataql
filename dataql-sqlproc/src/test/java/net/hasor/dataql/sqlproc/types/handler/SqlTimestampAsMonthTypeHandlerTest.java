package net.hasor.dataql.sqlproc.types.handler;

import java.sql.PreparedStatement;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import java.sql.ResultSet;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampAsMonthTypeHandler;
import org.junit.Test;

public class SqlTimestampAsMonthTypeHandlerTest extends AbstractSqlProcTest {
    @Test
    public void testMonth() throws Throwable {
        java.time.Month val = java.time.Month.MAY;
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new SqlTimestampAsMonthTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlTimestampAsMonthTypeHandler().getResult(rs, 1);
                assert res instanceof java.time.Month;
                assert val.equals(res);
            }
        }
    }
}
