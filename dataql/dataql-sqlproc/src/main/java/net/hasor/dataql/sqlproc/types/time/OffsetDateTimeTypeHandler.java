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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/** Reads ISO text with its offset intact and accepts ISO text or epoch milliseconds. */
public class OffsetDateTimeTypeHandler extends AbstractTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        if (parameter instanceof Number number) {
            OffsetDateTime value = Instant.ofEpochMilli(number.longValue()).atOffset(ZoneOffset.UTC);
            ps.setObject(i, value);
        } else {
            ps.setObject(i, this.parse(parameter.toString()));
        }
    }

    private OffsetDateTime parse(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        String text = value.trim().replace(' ', 'T');
        int offsetIndex = Math.max(text.lastIndexOf('+'), text.lastIndexOf('-'));
        if (offsetIndex > 0 && text.length() - offsetIndex == 5 && text.charAt(offsetIndex + 3) != ':') {
            text = text.substring(0, offsetIndex + 3) + ":" + text.substring(offsetIndex + 3);
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new SQLException("Invalid OffsetDateTime value: " + value, e);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        OffsetDateTime value;
        try {
            value = rs.getObject(columnName, OffsetDateTime.class);
        } catch (SQLException | AbstractMethodError e) {
            // Older drivers may expose time-zone values only as text.
            try {
                value = this.parse(rs.getString(columnName));
            } catch (SQLException failure) {
                failure.addSuppressed(e);
                throw failure;
            }
        }
        return value == null ? null : value.toString();
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        OffsetDateTime value;
        try {
            value = rs.getObject(columnIndex, OffsetDateTime.class);
        } catch (SQLException | AbstractMethodError e) {
            // Older drivers may expose time-zone values only as text.
            try {
                value = this.parse(rs.getString(columnIndex));
            } catch (SQLException failure) {
                failure.addSuppressed(e);
                throw failure;
            }
        }
        return value == null ? null : value.toString();
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        OffsetDateTime value;
        try {
            value = cs.getObject(columnIndex, OffsetDateTime.class);
        } catch (SQLException | AbstractMethodError e) {
            // Older drivers may expose time-zone values only as text.
            try {
                value = this.parse(cs.getString(columnIndex));
            } catch (SQLException failure) {
                failure.addSuppressed(e);
                throw failure;
            }
        }
        return value == null ? null : value.toString();
    }
}
