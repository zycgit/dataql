/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.segment;
import java.sql.SQLException;
import java.util.Collections;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.ArgRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

public record PositionSqlSegment(int position) implements SqlSegment {

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        ArgRule.INSTANCE.executeRule(data, context, sqlBuilder, "arg" + position, Collections.emptyMap());
    }

    @Override
    public PositionSqlSegment clone() {
        return new PositionSqlSegment(this.position);
    }

    @Override
    public String toString() {
        return "Args [" + this.position + "]";
    }
}
