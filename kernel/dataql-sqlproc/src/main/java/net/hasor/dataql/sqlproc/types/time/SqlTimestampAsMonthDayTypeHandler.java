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
import java.time.MonthDay;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 使用 {@link MonthDay} 类型读写 jdbc {@link java.sql.Timestamp} 数据。缺失的时间信息使用 0 补充。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2020-10-31
 */
public class SqlTimestampAsMonthDayTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        LocalDateTime dateTime = LocalDateTime.of(0, ((MonthDay) parameter).getMonth(), ((MonthDay) parameter).getDayOfMonth(), 0, 0);
        ps.setTimestamp(i, java.sql.Timestamp.valueOf(dateTime));
    }

    @Override
    public MonthDay getNullableResult(ResultSet rs, String columnName) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(columnName);
        return toMonthDay(timestamp);
    }

    @Override
    public MonthDay getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(columnIndex);
        return toMonthDay(timestamp);
    }

    @Override
    public MonthDay getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        java.sql.Timestamp timestamp = cs.getTimestamp(columnIndex);
        return toMonthDay(timestamp);
    }

    protected MonthDay toMonthDay(java.sql.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDate localDate = timestamp.toLocalDateTime().toLocalDate();
        return MonthDay.of(localDate.getMonth(), localDate.getDayOfMonth());
    }
}
