/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.vector;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import net.hasor.dataql.sqlproc.types.TypeHandler;

/**
 * JDBC conversion for PostgreSQL vector values.
 */
public class PgVectorTypeHandler implements TypeHandler {

    private static String toVectorString(List<?> vector) throws SQLException {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object value = vector.get(i);
            if (!(value instanceof Number) || !Float.isFinite(((Number) value).floatValue())) {
                throw new SQLException("Vector elements must be finite non-null numbers");
            }
            sb.append(((Number) value).floatValue());
        }
        sb.append(']');
        return sb.toString();
    }

    private static List<Number> parseVector(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        // Remove the vector delimiters.
        str = str.trim();
        if (str.startsWith("[")) {
            str = str.substring(1);
        }
        if (str.endsWith("]")) {
            str = str.substring(0, str.length() - 1);
        }
        if (str.isBlank()) {
            return new ArrayList<>();
        }
        String[] parts = str.split(",");
        List<Number> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(Float.parseFloat(part.trim()));
        }
        return result;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.OTHER);
        } else if (parameter instanceof List) {
            ps.setObject(i, toVectorString((List<?>) parameter), Types.OTHER);
        } else {
            throw new SQLException("Vector parameter must be a list of numbers.");
        }
    }

    @Override
    public List<Number> getResult(ResultSet rs, String columnName) throws SQLException {
        String val = rs.getString(columnName);
        return rs.wasNull() ? null : parseVector(val);
    }

    @Override
    public List<Number> getResult(ResultSet rs, int columnIndex) throws SQLException {
        String val = rs.getString(columnIndex);
        return rs.wasNull() ? null : parseVector(val);
    }

    @Override
    public List<Number> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String val = cs.getString(columnIndex);
        return cs.wasNull() ? null : parseVector(val);
    }
}
