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
import net.hasor.dataql.sqlproc.types.string.StringAsCharTypeHandler;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;
import org.junit.Test;

public class StringTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testStringTypeHandler_CallableStatement() throws Throwable {
        StringTypeHandler handler = new StringTypeHandler();
        Map<String, Object> values = new HashMap<>();
        String val = "hello";
        values.put("getString", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testStringAsCharTypeHandler_CallableStatement() throws Throwable {
        StringAsCharTypeHandler handler = new StringAsCharTypeHandler();
        Map<String, Object> values = new HashMap<>();
        String val = "abc";
        values.put("getString", val);

        CallableStatement cs = mockCallableStatement(values);
        Character result = (Character) handler.getResult(cs, 1);
        assert result == 'a';
    }

    @Test
    public void testString() throws Throwable {
        String val = "Hello World";
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_varchar) values (?)")) {
            new StringTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_varchar from tb_h2_types where c_varchar = ? limit 1")) {
            ps.setString(1, val);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new StringTypeHandler().getResult(rs, 1);
                assert val.equals(res);

                Object res2 = new StringTypeHandler().getResult(rs, "c_varchar");
                assert val.equals(res2);
            }
        }
    }

    @Test
    public void testStringAsChar() throws Throwable {
        Character val = 'H';
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_varchar) values (?)")) {
            new StringAsCharTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_varchar from tb_h2_types where c_varchar like ? limit 1")) {
            ps.setString(1, "H%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Character res = (Character) new StringAsCharTypeHandler().getResult(rs, 1);
                assert val.equals(res);
            }
        }
    }
}
