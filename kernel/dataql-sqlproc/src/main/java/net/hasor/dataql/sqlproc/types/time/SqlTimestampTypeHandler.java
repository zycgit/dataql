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
 * 读写 jdbc {@link java.sql.Timestamp} 数据。
 * @author Clinton Begin
 * @author 赵永春 (zyc@hasor.net)
 */
public class SqlTimestampTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setTimestamp(i, (java.sql.Timestamp) parameter);
    }

    @Override
    public java.sql.Timestamp getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getTimestamp(columnName);
    }

    @Override
    public java.sql.Timestamp getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getTimestamp(columnIndex);
    }

    @Override
    public java.sql.Timestamp getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getTimestamp(columnIndex);
    }
}
