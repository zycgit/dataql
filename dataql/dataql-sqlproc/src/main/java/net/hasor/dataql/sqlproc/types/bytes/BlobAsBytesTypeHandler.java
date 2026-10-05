/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.bytes;
import java.io.ByteArrayInputStream;
import java.sql.*;
import net.hasor.dataql.domain.BinaryValue;

/** Detaches BLOB content before the result set and connection are closed. */
public class BlobAsBytesTypeHandler extends BytesTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setBlob(i, new ByteArrayInputStream(this.toBytes(parameter)));
    }

    @Override
    public BinaryValue getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return this.readBlob(rs.getBlob(columnName));
    }

    private BinaryValue readBlob(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }
        try {
            long size = blob.length();
            if (size > Integer.MAX_VALUE) {
                throw new SQLException("BLOB exceeds the supported in-memory binary size");
            }
            return new BinaryValue(blob.getBytes(1, (int) size));
        } finally {
            blob.free();
        }
    }

    @Override
    public BinaryValue getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return this.readBlob(rs.getBlob(columnIndex));
    }

    @Override
    public BinaryValue getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return this.readBlob(cs.getBlob(columnIndex));
    }
}
