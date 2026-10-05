/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.sql.SQLException;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils;
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;
import net.hasor.dataql.sqlproc.dynamic.segment.PlanDynamicSql;
import net.hasor.dataql.sqlproc.types.SqlArg;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * 如果参数不为空，则生成 'and column = ?' 或者 'column = ?' 。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public abstract class ConditionRule implements SqlRule {
    protected static final String[] DEFAULT_TEST_PREFIX = new String[] { "where", ",", "and", "or", "not", "!" };
    private final          String[] testPrefix;
    private final          String   mustHave;
    private final          String   mustHaveAppend;
    private final          String   append;
    protected final        boolean  usingIf;

    protected ConditionRule(boolean usingIf, String[] testPrefix, String mustHave, String mustHaveAppend, String append) {
        this.usingIf = usingIf;
        this.testPrefix = testPrefix;
        this.mustHave = mustHave;
        this.mustHaveAppend = mustHaveAppend;
        this.append = append;
    }

    protected abstract String name();

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        if (this.usingIf) {
            return StringUtils.isBlank(activeExpr) || Boolean.TRUE.equals(OgnlUtils.evalOgnl(activeExpr, data));
        } else {
            return true;
        }
    }

    protected abstract boolean allowNullValue();

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) throws SQLException {
        String expr = "";
        if (this.usingIf) {
            expr = (StringUtils.isBlank(ruleValue) ? "" : ruleValue);
        } else {
            if (activeExpr != null) {
                expr += activeExpr;
                if (StringUtils.isNotBlank(ruleValue)) {
                    expr += ",";
                }
            }

            if (ruleValue != null) {
                expr += ruleValue;
            }
        }
        if (StringUtils.isBlank(expr)) {
            return;
        }

        PlanDynamicSql parsedSql = DynamicParsed.getParsedSql(expr);
        SqlBuilder tmp = parsedSql.buildQuery(data, context);
        String sqlString = tmp.getSqlString();
        Object[] sqlArgs = tmp.getArgs();

        if (StringUtils.isBlank(sqlString)) {
            return;
        }

        // Fix: Ensure we properly check nulls
        boolean allNulls = testNullValue(sqlArgs);
        if (!this.allowNullValue() && allNulls) {
            if (parsedSql.getInjectionList().isEmpty()) {
                return;
            }
        }

        String sql = sqlBuilder.getSqlString().toLowerCase();
        if (this.mustHave != null) {
            if (!sql.contains(this.mustHave)) {
                sqlBuilder.appendSql(this.mustHaveAppend);
                sql = sql + this.mustHaveAppend;
            }
        }

        for (String test : this.testPrefix) {
            if (sql.trim().endsWith(test)) {
                sqlBuilder.appendSql(sqlString, sqlArgs);
                return;
            }
        }

        sqlBuilder.appendSql(this.append);
        sqlBuilder.appendSql(sqlString, sqlArgs);
    }

    @Override
    public String toString() {
        return this.name() + " [" + this.hashCode() + "]";
    }

    private static boolean testNullValue(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                if (arg instanceof SqlArg) {
                    if (((SqlArg) arg).getValue() != null) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
