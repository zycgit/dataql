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
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

public class RuleSqlSegment implements SqlSegment {
    private final String ruleExpr;
    private final String ruleName;
    private final String activeExpr;
    private final String ruleValue;

    public RuleSqlSegment(String ruleExpr, String ruleName, String activeExpr, String ruleValue) {
        this.ruleExpr = ruleExpr;
        this.ruleName = ruleName;
        this.activeExpr = activeExpr;
        this.ruleValue = ruleValue;
    }

    public String getExpr() {
        return this.ruleExpr;
    }

    public String getRule() {
        return this.ruleName;
    }

    public String getActiveExpr() {
        return this.activeExpr;
    }

    public String getRuleValue() {
        return this.ruleValue;
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        SqlRule ruleByName = context.findRule(this.ruleName);
        if (ruleByName == null) {
            throw new UnsupportedOperationException("rule `" + this.ruleName + "` Unsupported.");
        }
        if (ruleByName.test(data, context, this.activeExpr)) {
            ruleByName.executeRule(data, context, sqlBuilder, this.activeExpr, this.ruleValue);
        }
    }

    @Override
    public RuleSqlSegment clone() {
        return new RuleSqlSegment(this.ruleExpr, this.ruleName, this.activeExpr, this.ruleValue);
    }

    @Override
    public String toString() {
        return "Rule [" + this.ruleName + ", body=" + this.ruleValue + "]";
    }
}
