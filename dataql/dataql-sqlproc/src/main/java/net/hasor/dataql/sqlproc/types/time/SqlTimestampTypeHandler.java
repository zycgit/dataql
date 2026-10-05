/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.time;
import java.sql.*;
import java.util.Date;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/** Binds epoch milliseconds or JDBC TIMESTAMP text; SQL dates become epoch milliseconds in DataQL. */
public class SqlTimestampTypeHandler extends AbstractTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setTimestamp(i, this.toJdbcValue(parameter));
    }

    private Timestamp toJdbcValue(Object value) throws SQLException {
        if (value instanceof Number number) {
            return new Timestamp(number.longValue());
        }
        if (value instanceof Date date) {
            return new Timestamp(date.getTime());
        }
        if (value instanceof CharSequence) {
            String text = value.toString().trim();
            try {
                return Timestamp.valueOf(text.replace('T', ' '));
            } catch (IllegalArgumentException e) {
                throw new SQLException("Invalid TIMESTAMP value: " + text, e);
            }
        }
        throw new SQLException("TIMESTAMP requires epoch milliseconds or text");
    }

    @Override
    public Timestamp getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getTimestamp(columnName);
    }

    @Override
    public Timestamp getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getTimestamp(columnIndex);
    }
}
