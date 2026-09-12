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
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 使用 {@link java.util.Date} 类型读写 jdbc {@link java.sql.Timestamp} 数据。
 * @author Clinton Begin
 * @author 赵永春 (zyc@hasor.net)
 */
public class SqlTimestampAsDateTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setTimestamp(i, new java.sql.Timestamp(((java.util.Date) parameter).getTime()));
    }

    @Override
    public java.util.Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        java.sql.Timestamp sqlTimestamp = rs.getTimestamp(columnName);
        if (sqlTimestamp != null) {
            return new java.util.Date(sqlTimestamp.getTime());
        }
        return null;
    }

    @Override
    public java.util.Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        java.sql.Timestamp sqlTimestamp = rs.getTimestamp(columnIndex);
        if (sqlTimestamp != null) {
            return new java.util.Date(sqlTimestamp.getTime());
        }
        return null;
    }

    @Override
    public java.util.Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        java.sql.Timestamp sqlTimestamp = cs.getTimestamp(columnIndex);
        if (sqlTimestamp != null) {
            return new java.util.Date(sqlTimestamp.getTime());
        }
        return null;
    }
}
