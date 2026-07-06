package net.hasor.dataql.sqlproc.types.handler;

import java.lang.reflect.Proxy;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.*;
import java.time.chrono.JapaneseDate;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.*;
import org.junit.Test;

public class TimeTypeHandlerTest extends AbstractSqlProcTest {

    @Test
    public void testSqlDateTypeHandler_CallableStatement() throws Throwable {
        SqlDateTypeHandler handler = new SqlDateTypeHandler();
        Map<String, Object> values = new HashMap<>();
        java.sql.Date val = new java.sql.Date(System.currentTimeMillis());
        values.put("getDate", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.toString().equals(result.toString());
    }

    @Test
    public void testSqlTimestampAsDateTypeHandler_CallableStatement() throws Throwable {
        SqlTimestampAsDateTypeHandler handler = new SqlTimestampAsDateTypeHandler(); // Uses getTimestamp usually
        Map<String, Object> values = new HashMap<>();
        Timestamp val = new Timestamp(System.currentTimeMillis());
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert result instanceof java.util.Date;
        assert val.getTime() == ((java.util.Date) result).getTime();
    }

    @Test
    public void testSqlTimestampTypeHandler_CallableStatement() throws Throwable {
        SqlTimestampTypeHandler handler = new SqlTimestampTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Timestamp val = new Timestamp(System.currentTimeMillis());
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testSqlTimeTypeHandler_CallableStatement() throws Throwable {
        SqlTimeTypeHandler handler = new SqlTimeTypeHandler();
        Map<String, Object> values = new HashMap<>();
        java.sql.Time val = new java.sql.Time(System.currentTimeMillis());
        values.put("getTime", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testSqlDate() throws Throwable {
        java.sql.Date val = new java.sql.Date(System.currentTimeMillis());
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_date) values (?)")) {
            new SqlDateTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_date from tb_h2_types where c_date is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlDateTypeHandler().getResult(rs, 1);
                assert res instanceof java.sql.Date;
                assert val.toString().equals(res.toString()); // Date comparison loose
            }
        }
    }

    @Test
    public void testUtilDate() throws Throwable {
        java.util.Date val = new java.util.Date();
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new SqlTimestampAsDateTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlTimestampAsDateTypeHandler().getResult(rs, 1);
                assert res instanceof java.util.Date;
                // Timestamp to Date might have precision issues or milliseconds
                assert Math.abs(val.getTime() - ((java.util.Date) res).getTime()) < 1000;
            }
        }
    }

    @Test
    public void testTimestamp() throws Throwable {
        Timestamp val = new Timestamp(System.currentTimeMillis());
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new SqlTimestampTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlTimestampTypeHandler().getResult(rs, 1);
                assert res instanceof Timestamp;
                assert Math.abs(val.getTime() - ((Timestamp) res).getTime()) < 1000;
            }
        }
    }

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

    @Test
    public void testLocalDateTime() throws Throwable {
        LocalDateTime val = LocalDateTime.now();
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new LocalDateTimeTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new LocalDateTimeTypeHandler().getResult(rs, 1);
                assert res instanceof LocalDateTime;
                assert val.getYear() == ((LocalDateTime) res).getYear();
                assert val.getMonth() == ((LocalDateTime) res).getMonth();
                assert val.getDayOfMonth() == ((LocalDateTime) res).getDayOfMonth();
            }
        }
    }

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

    @Test
    public void testJapaneseDateAsSqlDate() throws Throwable {
        JapaneseDateAsSqlDateTypeHandler handler = new JapaneseDateAsSqlDateTypeHandler();
        Map<String, Object> values = new HashMap<>();
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.now());
        values.put("getDate", sqlDate);

        CallableStatement cs = mockCallableStatement(values);
        JapaneseDate result = (JapaneseDate) handler.getResult(cs, 1);
        assert result != null;
        assert result.toString().equals(JapaneseDate.from(LocalDate.now()).toString());
    }

    @Test
    public void testLocalDateTimeAsLocalDate() throws Throwable {
        LocalDateTimeAsLocalDateTypeHandler handler = new LocalDateTimeAsLocalDateTypeHandler();
        Map<String, Object> values = new HashMap<>();
        LocalDateTime ldt = LocalDateTime.now();
        values.put("getObject", ldt);

        CallableStatement cs = mockCallableStatement(values);
        LocalDate result = (LocalDate) handler.getResult(cs, 1);
        assert result.equals(ldt.toLocalDate());
    }

    @Test
    public void testOffsetDateTime() throws Throwable {
        OffsetDateTimeTypeHandler handler = new OffsetDateTimeTypeHandler();
        Map<String, Object> values = new HashMap<>();
        OffsetDateTime val = OffsetDateTime.now();
        values.put("getObject", val);

        CallableStatement cs = mockCallableStatement(values);
        OffsetDateTime result = (OffsetDateTime) handler.getResult(cs, 1);
        assert val.equals(result);
    }

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
    public void testOffsetTime() throws Throwable {
        OffsetTimeTypeHandler handler = new OffsetTimeTypeHandler();
        Map<String, Object> values = new HashMap<>();
        OffsetTime val = OffsetTime.now();
        values.put("getObject", val);

        CallableStatement cs = mockCallableStatement(values);
        OffsetTime result = (OffsetTime) handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testSqlTimestampAsInstant() throws Throwable {
        SqlTimestampAsInstantTypeHandler handler = new SqlTimestampAsInstantTypeHandler();
        Map<String, Object> values = new HashMap<>();
        Timestamp val = Timestamp.from(Instant.now());
        values.put("getTimestamp", val);

        CallableStatement cs = mockCallableStatement(values);
        Instant result = (Instant) handler.getResult(cs, 1);
        assert val.toInstant().equals(result);
    }

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

    // --- Added Tests for Coverage ---

    protected PreparedStatement mockPreparedStatement(final Map<Integer, Object> captured) {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { PreparedStatement.class }, (proxy, method, args) -> {
            if (method.getName().startsWith("set") && args.length >= 2) {
                captured.put((Integer) args[0], args[1]);
            }
            return null;
        });
    }

    protected ResultSet mockResultSet(final Map<String, Object> values) {
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ResultSet.class }, (proxy, method, args) -> {
            String name = method.getName();
            if (name.startsWith("get")) {
                if (values.containsKey(name)) {
                    return values.get(name);
                }
                if (values.containsKey("default")) {
                    return values.get("default");
                }
            }
            if ("wasNull".equals(name)) {
                return values.getOrDefault("wasNull", false);
            }
            return null;
        });
    }

    @Test
    public void testJapaneseDateAsSqlDate_Full() throws Throwable {
        JapaneseDateAsSqlDateTypeHandler handler = new JapaneseDateAsSqlDateTypeHandler();

        // Static helpers
        assert JapaneseDateAsSqlDateTypeHandler.toJapaneseDate((java.sql.Date) null) == null;
        assert JapaneseDateAsSqlDateTypeHandler.toJapaneseDate((java.util.Date) null) == null;

        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2023, 1, 1));
        assert JapaneseDateAsSqlDateTypeHandler.toJapaneseDate(sqlDate).toString().equals(JapaneseDate.from(LocalDate.of(2023, 1, 1)).toString());
        assert JapaneseDateAsSqlDateTypeHandler.toJapaneseDate(new java.util.Date(sqlDate.getTime())).toString().equals(JapaneseDate.from(LocalDate.of(2023, 1, 1)).toString());

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        JapaneseDate jDate = JapaneseDate.from(LocalDate.of(2022, 2, 2));
        handler.setParameter(ps, 1, jDate, null);
        assert captured.get(1) instanceof java.sql.Date;
        assert captured.get(1).toString().equals("2022-02-02");

        // getResult
        Map<String, Object> values = new HashMap<>();
        values.put("getDate", java.sql.Date.valueOf("2023-03-03"));
        ResultSet rs = mockResultSet(values);
        JapaneseDate res = (JapaneseDate) handler.getResult(rs, 1);
        assert res.equals(JapaneseDate.from(LocalDate.of(2023, 3, 3)));
    }

    @Test
    public void testLocalDateTimeAsLocalDate_Full() throws Throwable {
        LocalDateTimeAsLocalDateTypeHandler handler = new LocalDateTimeAsLocalDateTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        LocalDate ld = LocalDate.of(2023, 4, 4);
        handler.setParameter(ps, 1, ld, null);
        // It sets Object: LocalDateTime
        Object capturedVal = captured.get(1);
        assert capturedVal instanceof LocalDateTime;
        assert ((LocalDateTime) capturedVal).toLocalDate().equals(ld);

        // getResult
        Map<String, Object> values = new HashMap<>();
        values.put("getObject", LocalDateTime.of(2023, 5, 5, 12, 0)); // Handler expects Object(LocalDateTime)
        values.put("default", LocalDateTime.of(2023, 5, 5, 12, 0));

        ResultSet rs = mockResultSet(values);
        LocalDate res = (LocalDate) handler.getResult(rs, "col");
        assert res.equals(LocalDate.of(2023, 5, 5));
    }

    @Test
    public void testOffsetDateTime_Full() throws Throwable {
        OffsetDateTimeTypeHandler handler = new OffsetDateTimeTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        OffsetDateTime odt = OffsetDateTime.now();
        handler.setParameter(ps, 1, odt, null);
        assert captured.get(1).equals(odt);

        // getResult
        Map<String, Object> values = new HashMap<>();
        values.put("getObject", odt); // for getObject(col, Class)
        values.put("default", odt);

        ResultSet rs = mockResultSet(values);
        OffsetDateTime res = (OffsetDateTime) handler.getResult(rs, 1);
        assert res.equals(odt);
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

    @Test
    public void testOffsetTime_Full() throws Throwable {
        OffsetTimeTypeHandler handler = new OffsetTimeTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        OffsetTime ot = OffsetTime.now();
        handler.setParameter(ps, 1, ot, null);
        assert captured.get(1).equals(ot);

        // getResult
        Map<String, Object> values = new HashMap<>();
        values.put("getObject", ot);
        values.put("default", ot);

        ResultSet rs = mockResultSet(values);
        OffsetTime res = (OffsetTime) handler.getResult(rs, 1);
        assert res.equals(ot);
    }

    @Test
    public void testSqlTimestampAsInstant_Full() throws Throwable {
        SqlTimestampAsInstantTypeHandler handler = new SqlTimestampAsInstantTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        Instant inst = Instant.now();
        handler.setParameter(ps, 1, inst, null);
        assert captured.get(1) instanceof Timestamp;
        assert ((Timestamp) captured.get(1)).toInstant().equals(inst);

        // getResult
        Map<String, Object> values = new HashMap<>();
        Timestamp ts = Timestamp.from(inst);
        values.put("getTimestamp", ts);

        ResultSet rs = mockResultSet(values);
        Instant res = (Instant) handler.getResult(rs, 1);
        assert res.equals(inst);
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
