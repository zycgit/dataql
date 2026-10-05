/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;
import static java.lang.reflect.Array.*;

/**
 * 读写 jdbc 数组类型
 * @author Clinton Begin
 * @author 赵永春 (zyc@hasor.net)
 */
public class ArrayTypeHandler extends AbstractTypeHandler<Object> {
    protected static final ConcurrentHashMap<Class<?>, JDBCType> STANDARD_MAPPING;

    static {
        STANDARD_MAPPING = new ConcurrentHashMap<>();
        STANDARD_MAPPING.put(boolean.class, JDBCType.BOOLEAN);
        STANDARD_MAPPING.put(Boolean.class, JDBCType.BOOLEAN);
        STANDARD_MAPPING.put(byte.class, JDBCType.TINYINT);
        STANDARD_MAPPING.put(Byte.class, JDBCType.TINYINT);
        STANDARD_MAPPING.put(short.class, JDBCType.SMALLINT);
        STANDARD_MAPPING.put(Short.class, JDBCType.SMALLINT);
        STANDARD_MAPPING.put(int.class, JDBCType.INTEGER);
        STANDARD_MAPPING.put(Integer.class, JDBCType.INTEGER);
        STANDARD_MAPPING.put(long.class, JDBCType.BIGINT);
        STANDARD_MAPPING.put(Long.class, JDBCType.BIGINT);
        STANDARD_MAPPING.put(float.class, JDBCType.FLOAT);
        STANDARD_MAPPING.put(Float.class, JDBCType.FLOAT);
        STANDARD_MAPPING.put(double.class, JDBCType.DOUBLE);
        STANDARD_MAPPING.put(Double.class, JDBCType.DOUBLE);
        // java extensions Types
        STANDARD_MAPPING.put(String.class, JDBCType.VARCHAR);
        STANDARD_MAPPING.put(BigInteger.class, JDBCType.NUMERIC);
        STANDARD_MAPPING.put(BigDecimal.class, JDBCType.NUMERIC);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        if (parameter instanceof Array) {
            // it's the user's responsibility to properly free() the Array instance
            ps.setArray(i, (Array) parameter);
        } else {
            Object[] elements;
            if (parameter instanceof Collection<?> values) {
                elements = values.toArray();
            } else if (parameter.getClass().isArray()) {
                elements = new Object[getLength(parameter)];
                for (int index = 0; index < elements.length; index++) {
                    elements[index] = get(parameter, index);
                }
            } else {
                throw new SQLException("SQL ARRAY requires a script list");
            }
            String arrayTypeName = this.elementType(elements);
            Array array = ps.getConnection().createArrayOf(arrayTypeName, elements);
            try {
                ps.setArray(i, array);
            } finally {
                array.free();
            }
        }
    }

    private String elementType(Object[] elements) throws SQLException {
        JDBCType type = null;
        for (Object value : elements) {
            if (value == null) {
                continue;
            }
            JDBCType current = STANDARD_MAPPING.get(value.getClass());
            if (current == null) {
                throw new SQLException("Unsupported SQL ARRAY element: " + value.getClass().getName());
            }
            if (type == null || type == current) {
                type = current;
            } else if (value instanceof Number && this.numeric(type)) {
                // A script list can contain mixed integer and decimal representations.
                if (type == JDBCType.NUMERIC || current == JDBCType.NUMERIC) {
                    type = JDBCType.NUMERIC;
                } else if (type == JDBCType.DOUBLE || current == JDBCType.DOUBLE || type == JDBCType.FLOAT || current == JDBCType.FLOAT) {
                    type = JDBCType.DOUBLE;
                } else {
                    type = JDBCType.BIGINT;
                }
            } else {
                throw new SQLException("SQL ARRAY elements must have compatible types");
            }
        }
        return type == null ? JDBCType.JAVA_OBJECT.getName() : type.getName();
    }

    private boolean numeric(JDBCType type) {
        return switch (type) {
            case TINYINT, SMALLINT, INTEGER, BIGINT, FLOAT, DOUBLE, NUMERIC -> true;
            default -> false;
        };
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array;
        try {
            array = rs.getArray(columnName);
        } catch (SQLException e) {
            if (rs.getObject(columnName) == null) {
                return null;
            }
            throw e;
        }
        return this.extractArray(array);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array;
        try {
            array = rs.getArray(columnIndex);
        } catch (SQLException e) {
            if (rs.getObject(columnIndex) == null) {
                return null;
            }
            throw e;
        }
        return this.extractArray(array);
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array;
        try {
            array = cs.getArray(columnIndex);
        } catch (SQLException e) {
            if (cs.getObject(columnIndex) == null) {
                return null;
            }
            throw e;
        }
        return this.extractArray(array);
    }

    protected Object extractArray(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        try {
            Object result = array.getArray();
            if (result instanceof Object[] values) {
                values = this.convertTemporalElements(values);
                Object[] typed = (Object[]) this.createTypedArrayFromSqlType(array.getBaseTypeName(), values.length);
                // Keep double precision; only SQL REAL/FLOAT4 arrays need conversion to Float.
                if (typed.getClass() == Object[].class && values.length > 0) {
                    for (Object value : values) {
                        if (value != null) {
                            typed = (Object[]) newInstance(value.getClass(), values.length);
                            break;
                        }
                    }
                }
                for (int i = 0; i < values.length; i++) {
                    Object value = values[i];
                    if (typed instanceof Float[] && value instanceof Number) {
                        value = ((Number) value).floatValue();
                    }
                    if (value != null && !typed.getClass().getComponentType().isInstance(value)) {
                        return values;
                    }
                    typed[i] = value;
                }
                return typed;
            }
            return result;
        } finally {
            array.free();
        }
    }

    private Object[] convertTemporalElements(Object[] values) {
        Object[] result = values;
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            Object converted = value;
            if (value instanceof LocalDate date) {
                converted = Date.valueOf(date).getTime();
            } else if (value instanceof LocalTime time) {
                converted = Time.valueOf(time).getTime();
            } else if (value instanceof LocalDateTime timestamp) {
                converted = Timestamp.valueOf(timestamp).getTime();
            } else if (value instanceof OffsetDateTime || value instanceof OffsetTime) {
                converted = value.toString();
            } else if (value instanceof Object[] nested) {
                converted = this.convertTemporalElements(nested);
            }
            if (converted != value) {
                if (result == values) {
                    result = Arrays.copyOf(values, values.length, Object[].class);
                }
                result[i] = converted;
            }
        }
        return result;
    }

    private Object createTypedArrayFromSqlType(String baseTypeName, int length) {
        if (baseTypeName == null) {
            return new Object[length];
        }
        return switch (baseTypeName.toUpperCase(Locale.ROOT)) {
            case "INTEGER", "INT", "INT4" -> new Integer[length];
            case "BIGINT", "LONG", "INT8" -> new Long[length];
            case "SMALLINT", "SHORT", "INT2" -> new Short[length];
            case "REAL", "FLOAT", "FLOAT4" -> new Float[length];
            case "DOUBLE", "DOUBLE PRECISION", "FLOAT8" -> new Double[length];
            case "NUMERIC", "DECIMAL" -> new BigDecimal[length];
            case "BOOLEAN", "BOOL" -> new Boolean[length];
            case "VARCHAR", "CHAR", "TEXT", "STRING", "BPCHAR" -> new String[length];
            default -> new Object[length];
        };
    }
}
