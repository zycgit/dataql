/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import net.hasor.dataql.sqlproc.types.ObjectTypeHandler;
import org.junit.Test;

public class ObjectTypeHandlerTest extends TypeHandlerMockSupport {
    @Test
    public void testObject() throws Throwable {
        String val = "testObject";
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_varchar) values (?)")) {
            new ObjectTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_varchar from tb_h2_types where c_varchar = ? limit 1")) {
            ps.setString(1, val);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new ObjectTypeHandler().getResult(rs, 1);
                assert val.equals(res);

                Object res2 = new ObjectTypeHandler().getResult(rs, "c_varchar");
                assert val.equals(res2);
            }
        }
    }
}
