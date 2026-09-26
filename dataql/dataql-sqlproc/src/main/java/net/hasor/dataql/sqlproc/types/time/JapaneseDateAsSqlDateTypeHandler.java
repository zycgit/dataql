/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.time;
import java.sql.*;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 使用 {@link JapaneseDate} 类型读写 jdbc {@link Date} 数据。
 * @author Kazuki Shimizu
 * @author 赵永春 (zyc@hasor.net)
 * @since 3.4.5
 */
public class JapaneseDateAsSqlDateTypeHandler extends AbstractTypeHandler {
    public static JapaneseDate toJapaneseDate(Date date) {
        if (date != null) {
            return JapaneseDate.from(date.toLocalDate());
        }
        return null;
    }

    public static JapaneseDate toJapaneseDate(java.util.Date date) {
        if (date != null) {
            return toJapaneseDate(new Date(date.getTime()));
        }
        return null;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setDate(i, Date.valueOf(LocalDate.ofEpochDay(((JapaneseDate) parameter).toEpochDay())));
    }

    @Override
    public JapaneseDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Date date = rs.getDate(columnName);
        return toJapaneseDate(date);
    }

    @Override
    public JapaneseDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Date date = rs.getDate(columnIndex);
        return toJapaneseDate(date);
    }

    @Override
    public JapaneseDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Date date = cs.getDate(columnIndex);
        return toJapaneseDate(date);
    }
}
