package net.hasor.dataql.sqlproc.types.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import net.hasor.dataql.sqlproc.types.time.LocalTimeTypeHandler;
import org.junit.Test;

public class LocalTimeTypeHandlerTest extends TypeHandlerMockSupport {
    @Test
    public void testLocalTime() throws Throwable {
        LocalTime val = LocalTime.now();
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_time) values (?)")) {
            new LocalTimeTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_time from tb_h2_types where c_time is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new LocalTimeTypeHandler().getResult(rs, 1);
                assert res instanceof LocalTime;
                assert val.getHour() == ((LocalTime) res).getHour();
                assert val.getMinute() == ((LocalTime) res).getMinute();
            }
        }
    }
}
