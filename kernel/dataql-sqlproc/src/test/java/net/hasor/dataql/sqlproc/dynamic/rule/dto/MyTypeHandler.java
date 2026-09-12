/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule.dto;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.hasor.dataql.sqlproc.types.TypeHandler;

public class MyTypeHandler implements TypeHandler {
    @Override
    public void setParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {

    }

    @Override
    public Object getResult(ResultSet rs, String columnName) throws SQLException {
        return null;
    }

    @Override
    public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return null;
    }
}
