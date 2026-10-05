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

/** Binds epoch milliseconds or JDBC TIME text; SQL dates become epoch milliseconds in DataQL. */
public class SqlTimeTypeHandler extends AbstractTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setTime(i, this.toJdbcValue(parameter));
    }

    private Time toJdbcValue(Object value) throws SQLException {
        if (value instanceof Number number) {
            return new Time(number.longValue());
        }
        if (value instanceof Date date) {
            return new Time(date.getTime());
        }
        if (value instanceof CharSequence) {
            String text = value.toString().trim();
            try {
                return Time.valueOf(text);
            } catch (IllegalArgumentException e) {
                throw new SQLException("Invalid TIME value: " + text, e);
            }
        }
        throw new SQLException("TIME requires epoch milliseconds or text");
    }

    @Override
    public Time getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getTime(columnName);
    }

    @Override
    public Time getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getTime(columnIndex);
    }

    @Override
    public Time getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getTime(columnIndex);
    }
}
