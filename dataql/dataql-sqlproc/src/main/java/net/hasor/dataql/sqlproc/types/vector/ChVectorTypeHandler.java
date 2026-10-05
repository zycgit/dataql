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
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;

/** Maps a float vector to ClickHouse Array(Float32), using JDBC array binding. */
public class ChVectorTypeHandler implements TypeHandler {
    @Override
    public void setParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        if (parameter == null) {
            throw new SQLException("ClickHouse Array(Float32) does not support a null vector.");
        }
        if (!(parameter instanceof List)) {
            throw new SQLException("Vector parameter must be a list of numbers.");
        }
        List<?> vector = (List<?>) parameter;
        Float[] values = new Float[vector.size()];
        for (int index = 0; index < values.length; index++) {
            Object value = vector.get(index);
            if (!(value instanceof Number) || !Float.isFinite(((Number) value).floatValue())) {
                throw new SQLException("Vector elements must be finite non-null numbers");
            }
            values[index] = ((Number) value).floatValue();
        }
        Array array = ps.getConnection().createArrayOf("Float32", values);
        try {
            ps.setArray(i, array);
        } finally {
            array.free();
        }
    }

    @Override
    public List<Number> getResult(ResultSet rs, String columnName) throws SQLException {
        return readVector(rs.getArray(columnName));
    }

    @Override
    public List<Number> getResult(ResultSet rs, int columnIndex) throws SQLException {
        return readVector(rs.getArray(columnIndex));
    }

    @Override
    public List<Number> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readVector(cs.getArray(columnIndex));
    }

    private List<Number> readVector(Array array) throws SQLException {
        if (array == null) {
            return null;
        }

        try {
            Object values = array.getArray();
            int size = getLength(values);
            List<Number> vector = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Object value = get(values, i);
                if (!(value instanceof Number)) {
                    throw new SQLException("ClickHouse vector elements must be numeric and non-null.");
                }
                vector.add(((Number) value).floatValue());
            }
            return vector;
        } finally {
            array.free();
        }
    }
}
