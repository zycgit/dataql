package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampAsMonthDayTypeHandler;
import org.junit.Test;

public class SqlTimestampAsMonthDayTypeHandlerTest extends AbstractSqlProcTest {
    @Test
    public void testSqlTimestampAsMonthDay() throws Throwable {
        SqlTimestampAsMonthDayTypeHandler handler = new SqlTimestampAsMonthDayTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Timestamp val = Timestamp.valueOf(LocalDateTime.of(2000, 12, 12, 0, 0));
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        MonthDay result = (MonthDay) handler.getResult(cs, 1);
        assert result.getMonthValue() == 12;
        assert result.getDayOfMonth() == 12;
    }

    @Test
    public void testSqlTimestampAsMonthDay_Full() throws Throwable {
        SqlTimestampAsMonthDayTypeHandler handler = new SqlTimestampAsMonthDayTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        MonthDay md = MonthDay.of(12, 12);
        handler.setParameter(ps, 1, md, null);
        assert captured.get(1) instanceof Timestamp;

        // getResult
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2000, 12, 12, 0, 0));
        Map<String, Object> values = new HashMap<>();
        values.put("getTimestamp", ts);

        ResultSet rs = mockResultSet(values);
        MonthDay res = (MonthDay) handler.getResult(rs, 1);
        assert res.equals(md);
    }
}
