package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.LocalDateTimeAsLocalDateTypeHandler;
import org.junit.Test;

public class LocalDateTimeAsLocalDateTypeHandlerTest extends AbstractHandlerTest {
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
}
