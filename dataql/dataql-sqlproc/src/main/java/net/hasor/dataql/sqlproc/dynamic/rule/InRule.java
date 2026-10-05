/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.args.ArraySqlArgSource;
import net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils;
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;
import net.hasor.dataql.sqlproc.types.SqlArg;
import net.hasor.dataql.sqlproc.types.SqlArgSource;
import net.hasor.dataql.sqlproc.types.SqlMode;
import net.hasor.dataql.sqlproc.types.TypeHandler;

/**
 * in 规则，用于自动生成 in 语句后的多重参数，例如： where col in (?,?,?,?)。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class InRule implements SqlRule {
    public static final SqlRule INSTANCE = new InRule(false);
    private final       boolean usingIf;

    public InRule(boolean usingIf) {
        this.usingIf = usingIf;
    }

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        if (this.usingIf) {
            return StringUtils.isBlank(activeExpr) || Boolean.TRUE.equals(OgnlUtils.evalOgnl(activeExpr, data));
        } else {
            return true;
        }
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) throws SQLException {
        String expr = "";
        if (this.usingIf) {
            expr = (StringUtils.isBlank(ruleValue) ? "" : ruleValue);
        } else {
            if (activeExpr != null) {
                expr += activeExpr;
                if (ruleValue != null) {
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

        SqlBuilder tmp = DynamicParsed.getParsedSql(expr).buildQuery(data, context);
        String sqlString = tmp.getSqlString();
        Object[] sqlArgs = tmp.getArgs();

        if (StringUtils.isBlank(sqlString) || sqlArgs.length == 0) {
            return;
        }

        if (sqlArgs.length > 1) {
            String inName = usingIf ? "IFIN" : "IN";
            throw new SQLException("role " + inName + " args error, require 1, but " + sqlArgs.length);
        }

        SqlBuilder buildIn = new SqlBuilder();
        buildIn.appendSql("(");
        buildIn(buildIn, sqlArgs[0]);
        buildIn.appendSql(")");

        sqlString = sqlString.replace("?", buildIn.getSqlString());
        sqlArgs = buildIn.getArgs();

        if (sqlArgs.length > 0) {
            sqlBuilder.appendSql(sqlString, sqlArgs);
        }
    }

    private static void buildIn(final SqlBuilder sqlBuilder, final Object value) {
        String name = null;
        Object tmpValue = value;
        Integer jdbcType = null;
        SqlMode sqlMode = null;
        TypeHandler typeHandler = null;
        boolean usingArgObj = false;

        if (value instanceof SqlArg) {
            name = ((SqlArg) value).getName();
            tmpValue = ((SqlArg) value).getValue();
            jdbcType = ((SqlArg) value).getJdbcType();
            sqlMode = ((SqlArg) value).getSqlMode();
            typeHandler = ((SqlArg) value).getTypeHandler();
            usingArgObj = true;
        }
        if (tmpValue != null && tmpValue.getClass().isArray()) {
            tmpValue = Arrays.asList(ArraySqlArgSource.toArgs(tmpValue));
        }

        if (tmpValue instanceof Iterable) {
            Iterator<?> entryIter = ((Iterable<?>) tmpValue).iterator();
            int k = 0;
            while (entryIter.hasNext()) {
                String term = (k == 0) ? "?" : ", ?";
                if (usingArgObj) {
                    sqlBuilder.appendSql(term, new SqlArg(name + "[" + k + "]", entryIter.next(), sqlMode, jdbcType, typeHandler));
                } else {
                    sqlBuilder.appendSql(term, entryIter.next());
                }

                k++;
            }
        } else if (tmpValue != null) {
            sqlBuilder.appendSql("?", value);
        }
    }

    @Override
    public String toString() {
        return (this.usingIf ? "ifin [" : "in [") + this.hashCode() + "]";
    }
}
