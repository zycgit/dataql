/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.segment;
import java.sql.SQLException;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.types.SqlArgSource;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;

public class TextSqlSegment implements SqlSegment {
    private final StringBuilder textString;

    public TextSqlSegment(String exprString) {
        this.textString = new StringBuilder(exprString);
    }

    public void append(String append) {
        this.textString.append(append);
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        sqlBuilder.appendSql(this.textString.toString());
    }

    @Override
    public TextSqlSegment clone() {
        return new TextSqlSegment(this.textString.toString());
    }

    @Override
    public String toString() {
        return "Text [" + this.textString + "]";
    }
}
