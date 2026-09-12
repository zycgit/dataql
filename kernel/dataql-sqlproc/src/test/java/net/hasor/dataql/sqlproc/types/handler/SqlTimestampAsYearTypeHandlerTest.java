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
import java.time.Year;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampAsYearTypeHandler;
import org.junit.Test;

public class SqlTimestampAsYearTypeHandlerTest extends TypeHandlerMockSupport {
    @Test
    public void testYear() throws Throwable {
        Year val = Year.now();
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_timestamp) values (?)")) {
            new SqlTimestampAsYearTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("select c_timestamp from tb_h2_types where c_timestamp is not null limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new SqlTimestampAsYearTypeHandler().getResult(rs, 1);
                assert res instanceof Year;
                assert val.equals(res);
            }
        }
    }
}
