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
import net.hasor.dataql.sqlproc.types.number.FloatTypeHandler;
import org.junit.Test;

public class FloatTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testFloatTypeHandler_CallableStatement() throws Throwable {
        FloatTypeHandler handler = new FloatTypeHandler();
        Map<String, Object> values = new HashMap<>();
        values.put("getFloat", 123.45f);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Float.valueOf(123.45f).equals(result);
    }

    @Test
    public void testFloat() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_real) values (?)")) {
            new FloatTypeHandler().setParameter(ps, 1, 123.45f, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_real from tb_h2_types where c_real > 123 and c_real < 124 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new FloatTypeHandler().getResult(rs, 1);
                assert res instanceof Float;
                assert Math.abs(((Float) res) - 123.45f) < 0.0001;
            }
        }
    }
}
