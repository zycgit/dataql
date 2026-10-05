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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.bytes.BytesTypeHandler;
import org.junit.Test;
import net.hasor.dataql.domain.BinaryModel;

public class BytesTypeHandlerTest extends TypeHandlerMockSupport {

    @Test
    public void testBytesTypeHandler_CallableStatement() throws Throwable {
        BytesTypeHandler handler = new BytesTypeHandler();
        Map<String, Object> values = new HashMap<>();
        byte[] val = new byte[] { 1, 2, 3 };
        values.put("getBytes", val);

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Arrays.equals(val, ((BinaryModel) result).openStream().readAllBytes());
    }

    @Test
    public void testBytes() throws Throwable {
        byte[] val = new byte[] { 1, 2, 3, 4 };
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_varbinary) values (?)")) {
            new BytesTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_varbinary from tb_h2_types where c_varbinary is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new BytesTypeHandler().getResult(rs, 1);
                assert res instanceof BinaryModel;
                assert Arrays.equals(val, ((BinaryModel) res).openStream().readAllBytes());
            }
        }
    }

}
