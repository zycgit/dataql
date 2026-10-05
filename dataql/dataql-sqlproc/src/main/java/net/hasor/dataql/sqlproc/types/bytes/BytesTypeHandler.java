/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.bytes;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataql.domain.BinaryValue;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/** JDBC binary values remain binary when passed through the script data model. */
public class BytesTypeHandler extends AbstractTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setBytes(i, this.toBytes(parameter));
    }

    protected byte[] toBytes(Object value) throws SQLException {
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        if (value instanceof BinaryModel binary) {
            // Materialize before binding: JDBC may read the parameter only during execute().
            try (InputStream input = binary.openStream()) {
                return input.readAllBytes();
            } catch (IOException e) {
                throw new SQLException("Cannot read binary SQL parameter", e);
            }
        }
        throw new SQLException("Binary SQL parameters require BinaryModel or byte[]");
    }

    @Override
    public BinaryValue getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] value = rs.getBytes(columnName);
        return value == null ? null : new BinaryValue(value);
    }

    @Override
    public BinaryValue getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] value = rs.getBytes(columnIndex);
        return value == null ? null : new BinaryValue(value);
    }

    @Override
    public BinaryValue getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] value = cs.getBytes(columnIndex);
        return value == null ? null : new BinaryValue(value);
    }
}
