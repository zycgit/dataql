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
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.time.OffsetDateTimeTypeHandler;
import org.junit.Test;

public class OffsetDateTimeTypeHandlerTest extends TypeHandlerMockSupport {
    @Test
    public void testOffsetDateTime() throws Throwable {
        OffsetDateTimeTypeHandler handler = new OffsetDateTimeTypeHandler();
        Map<String, Object> values = new HashMap<>();
        OffsetDateTime val = OffsetDateTime.now();
        values.put("getObject", val);

        CallableStatement cs = mockCallableStatement(values);
        String result = (String) handler.getResult(cs, 1);
        assert val.toString().equals(result);
    }

    @Test
    public void testOffsetDateTime_Full() throws Throwable {
        OffsetDateTimeTypeHandler handler = new OffsetDateTimeTypeHandler();

        // setParameter
        Map<Integer, Object> captured = new HashMap<>();
        PreparedStatement ps = mockPreparedStatement(captured);
        OffsetDateTime odt = OffsetDateTime.now();
        handler.setParameter(ps, 1, odt.toString(), null);
        assert captured.get(1).equals(odt);

        // getResult
        Map<String, Object> values = new HashMap<>();
        values.put("getObject", odt); // for getObject(col, Class)
        values.put("default", odt);

        ResultSet rs = mockResultSet(values);
        String res = (String) handler.getResult(rs, 1);
        assert res.equals(odt.toString());
    }
}
