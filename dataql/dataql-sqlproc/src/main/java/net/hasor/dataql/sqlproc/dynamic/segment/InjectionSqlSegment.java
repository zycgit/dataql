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
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.types.SqlArgSource;
import static net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils.evalOgnl;

public class InjectionSqlSegment implements SqlSegment {
    private final String exprString;

    public InjectionSqlSegment(String exprString) {
        this.exprString = exprString;
    }

    public String getExpr() {
        return this.exprString;
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        sqlBuilder.appendSql(String.valueOf(evalOgnl(this.exprString, data)));
    }

    @Override
    public InjectionSqlSegment clone() {
        return new InjectionSqlSegment(this.exprString);
    }

    @Override
    public String toString() {
        return "Injection [" + this.exprString + "]";
    }
}
