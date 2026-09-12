/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.custom;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;

public class MyStringTypeHandler1 extends StringTypeHandler {
    private boolean writeMark;
    private boolean readMark;

    public boolean isWriteMark() {
        return writeMark;
    }

    public void setWriteMark(boolean writeMark) {
        this.writeMark = writeMark;
    }

    public boolean isReadMark() {
        return readMark;
    }

    public void setReadMark(boolean readMark) {
        this.readMark = readMark;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        this.writeMark = true;
        super.setNonNullParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        this.readMark = true;
        return super.getNullableResult(rs, columnName);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        this.readMark = true;
        return super.getNullableResult(rs, columnIndex);
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        this.readMark = true;
        return super.getNullableResult(cs, columnIndex);
    }
}
