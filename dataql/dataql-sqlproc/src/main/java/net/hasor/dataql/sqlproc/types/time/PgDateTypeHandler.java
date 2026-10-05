/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.time;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;
import net.hasor.dataql.sqlproc.types.NoCache;

/**
 * JDBC conversion for Pg Date values.
 * @author 赵永春 (zyc@hasor.net)
 * @version 2026-02-07
 */
@NoCache
public class PgDateTypeHandler extends AbstractTypeHandler<String> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String value, Integer jdbcType) throws SQLException {
        LocalDate parameter = LocalDate.parse(value);
        if (parameter.getYear() <= 0) {
            // ISO year 0 is 1 BC; negative years count backwards from it.
            int bcYear = Math.abs(parameter.getYear()) + 1;
            String bcDate = String.format("%04d-%02d-%02d BC", bcYear, parameter.getMonthValue(), parameter.getDayOfMonth());
            // Bind a PostgreSQL DATE value including its era suffix.
            ps.setObject(i, bcDate, Types.DATE);
        } else {
            // Common-era dates use the standard JDBC representation.
            ps.setDate(i, Date.valueOf(parameter));
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (value == null) {
            return null;
        }
        return parseDate(value).toString();
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        return parseDate(value).toString();
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        return parseDate(value).toString();
    }

    private LocalDate parseDate(String value) {
        if (value.endsWith(" BC")) {
            // ISO year 0 is 1 BC; negative years count backwards from it.
            String dateStr = value.substring(0, value.length() - 3).trim();
            LocalDate bcDate = LocalDate.parse(dateStr, FORMATTER);
            int isoYear = -(bcDate.getYear() - 1);
            return LocalDate.of(isoYear, bcDate.getMonth(), bcDate.getDayOfMonth());
        } else {
            // Common-era dates use the standard JDBC representation.
            return LocalDate.parse(value, FORMATTER);
        }
    }
}
