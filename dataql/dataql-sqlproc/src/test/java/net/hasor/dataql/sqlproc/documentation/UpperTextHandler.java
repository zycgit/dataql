/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.documentation;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;

/** Converts a bound string to uppercase while keeping standard string reads. */
public class UpperTextHandler extends StringTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String value, Integer jdbcType) throws SQLException {
        statement.setString(index, value.toString().toUpperCase(Locale.ROOT));
    }
}
