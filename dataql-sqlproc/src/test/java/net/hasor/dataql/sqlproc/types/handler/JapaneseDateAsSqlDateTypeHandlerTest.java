package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.JapaneseDateAsSqlDateTypeHandler;
import org.junit.Test;

public class JapaneseDateAsSqlDateTypeHandlerTest extends TypeHandlerMockSupport {

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
    public void testJapaneseDateAsSqlDate_Full() throws Throwable {
        JapaneseDateAsSqlDateTypeHandler handler = new JapaneseDateAsSqlDateTypeHandler();

        // Static helpers
        assert JapaneseDateAsSqlDateTypeHandler.toJapaneseDate(null) == null;
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
}
