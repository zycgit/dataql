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
import java.util.Map;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.ArgRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

public class NamedSqlSegment implements SqlSegment {
    private final String              exprString;
    private final Map<String, String> config;

    public NamedSqlSegment(String exprString) {
        this(exprString, Collections.emptyMap());
    }

    public NamedSqlSegment(String exprString, Map<String, String> config) {
        this.exprString = exprString;
        this.config = config;
    }

    public String getExpr() {
        return this.exprString;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        ArgRule.INSTANCE.executeRule(data, context, sqlBuilder, this.exprString, this.config);
    }

    @Override
    public NamedSqlSegment clone() {
        return new NamedSqlSegment(this.exprString, this.config);
    }

    @Override
    public String toString() {
        return "Named [" + this.exprString + "]";
    }
}
