/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.handler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.number.BigIntegerTypeHandler;
import org.junit.Test;

public class BigIntegerTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testBigIntegerTypeHandler_CallableStatement() throws Throwable {
        BigIntegerTypeHandler handler = new BigIntegerTypeHandler();
        Map<String, Object> values = new HashMap<>();
        BigInteger val = new BigInteger("12345");
        // JDBC getBigDecimal is often used for BigInteger if getBigInteger doesn't exist or is standard
        // BigIntegerTypeHandler usually uses getBigDecimal to retrieve values if direct support isn't there
        // Let's check implementation if needed, but usually getBigDecimal is safe for mock
        values.put("getBigDecimal", new BigDecimal(val));

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testBigInteger() throws Throwable {
        BigInteger val = new BigInteger("1234567890123");
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_bigint) values (?)")) {
            new BigIntegerTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_bigint from tb_h2_types where c_bigint = 1234567890123 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new BigIntegerTypeHandler().getResult(rs, 1);
                assert res instanceof BigInteger;
                assert res.equals(val);
            }
        }
    }
}
