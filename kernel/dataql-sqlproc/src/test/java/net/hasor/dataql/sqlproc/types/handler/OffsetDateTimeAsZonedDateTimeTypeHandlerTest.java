package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.OffsetDateTimeAsZonedDateTimeTypeHandler;
import org.junit.Test;

public class OffsetDateTimeAsZonedDateTimeTypeHandlerTest extends TypeHandlerMockSupport {
    @Test
    public void testOffsetDateTimeAsZonedDateTime() throws Throwable {
        OffsetDateTimeAsZonedDateTimeTypeHandler handler = new OffsetDateTimeAsZonedDateTimeTypeHandler();
        Map<String, Object> values = new HashMap<>();
        OffsetDateTime val = OffsetDateTime.now();
        values.put("getObject", val);

        CallableStatement cs = mockCallableStatement(values);
        ZonedDateTime result = (ZonedDateTime) handler.getResult(cs, 1);
        assert result.toInstant().equals(val.toInstant());
    }

    @Test
    public void testOffsetDateTimeAsZonedDateTime_Full() throws Throwable {
        OffsetDateTimeAsZonedDateTimeTypeHandler handler = new OffsetDateTimeAsZonedDateTimeTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        ZonedDateTime zdt = ZonedDateTime.now();
        handler.setParameter(ps, 1, zdt, null);
        assert captured.get(1) instanceof OffsetDateTime; // checks conversion if any, actually implementation might cast/convert

        // check impl:
        // public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        //    ps.setObject(i, ((ZonedDateTime) parameter).toOffsetDateTime());
        // }

        // getResult
        OffsetDateTime odt = zdt.toOffsetDateTime();
        Map<String, Object> values = new HashMap<>();
        values.put("getObject", odt);
        values.put("default", odt);

        ResultSet rs = mockResultSet(values);
        ZonedDateTime res = (ZonedDateTime) handler.getResult(rs, 1);
        assert res.toInstant().equals(zdt.toInstant());
    }
}
