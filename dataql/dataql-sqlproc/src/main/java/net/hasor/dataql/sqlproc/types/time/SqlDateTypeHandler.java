/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.time;
import java.sql.*;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/** Binds epoch milliseconds or JDBC DATE text; SQL dates become epoch milliseconds in DataQL. */
public class SqlDateTypeHandler extends AbstractTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setDate(i, this.toJdbcValue(parameter));
    }

    private Date toJdbcValue(Object value) throws SQLException {
        if (value instanceof Number number) {
            return new Date(number.longValue());
        }
        if (value instanceof java.util.Date date) {
            return new Date(date.getTime());
        }
        if (value instanceof CharSequence) {
            String text = value.toString().trim();
            try {
                return Date.valueOf(text);
            } catch (IllegalArgumentException e) {
                throw new SQLException("Invalid DATE value: " + text, e);
            }
        }
        throw new SQLException("DATE requires epoch milliseconds or text");
    }

    @Override
    public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getDate(columnName);
    }

    @Override
    public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getDate(columnIndex);
    }

    @Override
    public Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getDate(columnIndex);
    }
}
