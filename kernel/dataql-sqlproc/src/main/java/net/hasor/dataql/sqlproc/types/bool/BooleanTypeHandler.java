/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.bool;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 读写 boolean 类型数据
 * @author Clinton Begin
 * @author 赵永春 (zyc@hasor.net)
 */
public class BooleanTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setBoolean(i, (Boolean) parameter);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        boolean result = rs.getBoolean(columnName);
        return !result && rs.wasNull() ? null : result;
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean result = rs.getBoolean(columnIndex);
        return !result && rs.wasNull() ? null : result;
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        boolean result = cs.getBoolean(columnIndex);
        return !result && cs.wasNull() ? null : result;
    }
}
