/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.time;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 读写 {@link LocalTime}  类型数据
 * @author 赵永春 (zyc@hasor.net)
 */
public class LocalTimeTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setObject(i, (LocalTime) parameter);
    }

    @Override
    public LocalTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, LocalTime.class);
    }

    @Override
    public LocalTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getObject(columnIndex, LocalTime.class);
    }

    @Override
    public LocalTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getObject(columnIndex, LocalTime.class);
    }
}
