/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.number.IntegerTypeHandler;
import org.junit.Test;

public class IntegerTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testIntegerTypeHandler_CallableStatement() throws Throwable {
        IntegerTypeHandler handler = new IntegerTypeHandler();
        Map<String, Object> values = new HashMap<>();
        values.put("getInt", 123456);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Integer.valueOf(123456).equals(result);
    }

    @Test
    public void testInt() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_integer) values (?)")) {
            new IntegerTypeHandler().setParameter(ps, 1, 123456, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_integer from tb_h2_types where c_integer = 123456 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new IntegerTypeHandler().getResult(rs, 1);
                assert res instanceof Integer;
                assert ((Integer) res) == 123456;

                Object res2 = new IntegerTypeHandler().getResult(rs, "c_integer");
                assert ((Integer) res2) == 123456;
            }
        }
    }
}
