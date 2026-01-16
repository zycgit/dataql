package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampAsYearMonthTypeHandler;
import org.junit.Test;

public class SqlTimestampAsYearMonthTypeHandlerTest extends AbstractHandlerTest {
    @Test
    public void testSqlTimestampAsYearMonthTypeHandler() throws Throwable {
        SqlTimestampAsYearMonthTypeHandler handler = new SqlTimestampAsYearMonthTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Timestamp val = Timestamp.valueOf(LocalDateTime.of(2000, 12, 1, 0, 0));
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        YearMonth result = (YearMonth) handler.getResult(cs, 1);
        assert result.getYear() == 2000;
        assert result.getMonthValue() == 12;
    }

    @Test
    public void testSqlTimestampAsYearMonth_Full() throws Throwable {
        SqlTimestampAsYearMonthTypeHandler handler = new SqlTimestampAsYearMonthTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        YearMonth ym = YearMonth.of(2023, 10);
        handler.setParameter(ps, 1, ym, null);
        assert captured.get(1) instanceof Timestamp;

        // getResult
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2023, 10, 1, 0, 0));
        Map<String, Object> values = new HashMap<>();
        values.put("getTimestamp", ts);

        ResultSet rs = mockResultSet(values);
        YearMonth res = (YearMonth) handler.getResult(rs, 1);
        assert res.equals(ym);
    }
}
