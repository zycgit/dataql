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
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

/**
 * 使用 {@link ZonedDateTime} 类型读写 jdbc {@link OffsetDateTime} 数据。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2020-10-31
 */
public class OffsetDateTimeAsZonedDateTimeTypeHandler extends AbstractTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
        ps.setObject(i, ((ZonedDateTime) parameter).toOffsetDateTime());
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        OffsetDateTime offsetDateTime = rs.getObject(columnName, OffsetDateTime.class);
        return (offsetDateTime == null) ? null : offsetDateTime.toZonedDateTime();
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        OffsetDateTime offsetDateTime = rs.getObject(columnIndex, OffsetDateTime.class);
        return (offsetDateTime == null) ? null : offsetDateTime.toZonedDateTime();
    }

    @Override
    public ZonedDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        OffsetDateTime offsetDateTime = cs.getObject(columnIndex, OffsetDateTime.class);
        return (offsetDateTime == null) ? null : offsetDateTime.toZonedDateTime();
    }
}
