package net.hasor.dataql.sqlproc.types.handler;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.number.BigDecimalTypeHandler;
import net.hasor.dataql.sqlproc.types.number.PgMoneyAsBigDecimalTypeHandler;
import org.junit.Test;

public class BigDecimalTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testPgMoneyAsBigDecimalTypeHandler_CallableStatement() throws Throwable {
        PgMoneyAsBigDecimalTypeHandler handler = new PgMoneyAsBigDecimalTypeHandler();
        Map<String, Object> values = new HashMap<>();
        String val = "$1,234.56";
        values.put("getString", val);

        CallableStatement cs = mockCallableStatement(values);
        BigDecimal result = (BigDecimal) handler.getResult(cs, 1);
        assert result.compareTo(new BigDecimal("1234.56")) == 0;
    }

    @Test
    public void testPgMoneyUtils() {
        assert PgMoneyAsBigDecimalTypeHandler.toNumber(null) == null;
        BigDecimal zeroResult = PgMoneyAsBigDecimalTypeHandler.toNumber("");
        assert zeroResult == null; // Logic check: blank string returns null in filerMoneySign, so toNumber returns null?
        // Wait, filerMoneySign returns null if blank. toNumber checks isBlank on moneySign.
        // If filerMoneySign returns null, toNumber new BigDecimal(null)?? No.
        // Let's re-read code.

        // Code: String moneySign = filerMoneySign(moneyValue);
        // Code: return StringUtils.isBlank(moneySign) ? null : new BigDecimal(moneySign);

        // if moneyValue is "", filerMoneySign returns null.
        // StringUtils.isBlank(null) is true.
        // returns null.

        // Correct test:
        assert PgMoneyAsBigDecimalTypeHandler.toNumber("") == null;
        assert PgMoneyAsBigDecimalTypeHandler.toNumber("  ") == null;
        assert PgMoneyAsBigDecimalTypeHandler.toNumber("$100.00").compareTo(new BigDecimal("100.00")) == 0;
        assert PgMoneyAsBigDecimalTypeHandler.toNumber("100.00").compareTo(new BigDecimal("100.00")) == 0;
        assert PgMoneyAsBigDecimalTypeHandler.toNumber("£1,000.50").compareTo(new BigDecimal("1000.50")) == 0;
    }

    @Test
    public void testBigDecimalTypeHandler_CallableStatement() throws Throwable {
        BigDecimalTypeHandler handler = new BigDecimalTypeHandler();
        Map<String, Object> values = new HashMap<>();
        BigDecimal val = new BigDecimal("123.45");
        values.put("getBigDecimal", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testBigDecimal() throws Throwable {
        BigDecimal val = new BigDecimal("123.45678");
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_numeric_10) values (?)")) {
            new BigDecimalTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_numeric_10 from tb_h2_types where c_numeric_10 is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new BigDecimalTypeHandler().getResult(rs, 1);
                assert res instanceof BigDecimal;
                assert ((BigDecimal) res).compareTo(val) == 0;
            }
        }
    }
}
