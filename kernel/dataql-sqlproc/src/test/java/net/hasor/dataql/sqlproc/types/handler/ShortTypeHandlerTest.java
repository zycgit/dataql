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
import net.hasor.dataql.sqlproc.types.number.ShortTypeHandler;
import org.junit.Test;

public class ShortTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testShortTypeHandler_CallableStatement() throws Throwable {
        ShortTypeHandler handler = new ShortTypeHandler();
        Map<String, Object> values = new HashMap<>();
        values.put("getShort", (short) 1234);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Short.valueOf((short) 1234).equals(result);
    }

    @Test
    public void testShort() throws Throwable {
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_smallint) values (?)")) {
            new ShortTypeHandler().setParameter(ps, 1, (short) 12345, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_smallint from tb_h2_types where c_smallint = 12345 limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new ShortTypeHandler().getResult(rs, 1);
                assert res instanceof Short;
                assert ((Short) res) == 12345;

                Object res2 = new ShortTypeHandler().getResult(rs, "c_smallint");
                assert ((Short) res2) == 12345;
            }
        }
    }
}
