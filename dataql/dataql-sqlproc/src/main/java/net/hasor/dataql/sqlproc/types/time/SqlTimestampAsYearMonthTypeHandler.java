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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 使用 {@link YearMonth} 类型读写 jdbc {@link Timestamp} 数据。缺失的时间信息使用 0 补充，月份/日期使用 1。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2020-10-31
 */
public class SqlTimestampAsYearMonthTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        LocalDateTime dateTime = LocalDateTime.of(((YearMonth) parameter).getYear(), ((YearMonth) parameter).getMonth(), 1, 0, 0);
        ps.setTimestamp(i, Timestamp.valueOf(dateTime));
    }

    @Override
    public YearMonth getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return toYearMonth(timestamp);
    }

    @Override
    public YearMonth getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return toYearMonth(timestamp);
    }

    @Override
    public YearMonth getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp timestamp = cs.getTimestamp(columnIndex);
        return toYearMonth(timestamp);
    }

    protected YearMonth toYearMonth(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDate localDate = timestamp.toLocalDateTime().toLocalDate();
        return YearMonth.of(localDate.getYear(), localDate.getMonthValue());
    }
}
