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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 使用 {@link LocalDateTime} 类型读写 jdbc {@link LocalDate} 数据。缺失的时间信息使用 00:00 补充。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2020-10-31
 */
public class LocalDateTimeAsLocalDateTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setObject(i, LocalDateTime.of((LocalDate) parameter, LocalTime.of(0, 0, 0, 0)));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        LocalDateTime dateTime = rs.getObject(columnName, LocalDateTime.class);
        return (dateTime == null) ? null : dateTime.toLocalDate();
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        LocalDateTime dateTime = rs.getObject(columnIndex, LocalDateTime.class);
        return (dateTime == null) ? null : dateTime.toLocalDate();
    }

    @Override
    public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        LocalDateTime dateTime = cs.getObject(columnIndex, LocalDateTime.class);
        return (dateTime == null) ? null : dateTime.toLocalDate();
    }
}
