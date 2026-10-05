/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.args.StackSqlArgSource;
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;
import net.hasor.dataql.sqlproc.dynamic.segment.PlanDynamicSql;
import net.hasor.dataql.sqlproc.types.SqlArg;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * Dynamic SQL pairs rule.
 * @author 赵永春 (zyc@hasor.net)
 * @version 2025-11-12
 */
public class PairsRule implements SqlRule {
    public static final SqlRule INSTANCE = new PairsRule();

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) throws SQLException {
        SqlBuilder builder = DynamicParsed.getParsedSql(activeExpr).buildQuery(data, context);
        Object[] args = builder.getArgs();
        if (args.length != 1) {
            throw new SQLException("role PAIRS args error, require 1, but " + args.length);
        }

        if (args[0] == null) {
            return;
        }

        PlanDynamicSql pairTemplate = DynamicParsed.getParsedSql(ruleValue);
        Object argValue = args[0] instanceof SqlArg ? ((SqlArg) args[0]).getValue() : args[0];
        if (argValue == null) {
            return;
        }
        if (argValue instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) argValue;
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                this.buildPair(sqlBuilder, pairTemplate, data, context, entry.getKey(), entry.getValue(), index++);
            }
        } else if (argValue instanceof Iterable<?>) {
            Iterable<?> it = (Iterable<?>) argValue;
            Iterator<?> iterator = it.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                int keyIndex = index++;
                Object value = iterator.next();
                this.buildPair(sqlBuilder, pairTemplate, data, context, String.valueOf(keyIndex), value, keyIndex);
            }
        } else if (argValue.getClass().isArray()) {
            int length = Array.getLength(argValue);
            for (int keyIndex = 0; keyIndex < length; keyIndex++) {
                Object obj = Array.get(argValue, keyIndex);
                this.buildPair(sqlBuilder, pairTemplate, data, context, String.valueOf(keyIndex), obj, keyIndex);
            }
        } else {
            throw new SQLException("role PAIRS require Map type parameter, but " + argValue.getClass().getName());
        }
    }

    private void buildPair(SqlBuilder sqlBuilder, PlanDynamicSql pairTemplate, SqlArgSource data, QueryContext context, Object k, Object v, int i) throws SQLException {
        StackSqlArgSource tmpSource = new StackSqlArgSource(data);
        tmpSource.putValue("k", k);
        tmpSource.putValue("v", v);
        tmpSource.putValue("i", i);

        SqlBuilder tmp = pairTemplate.buildQuery(tmpSource, context);
        if (!sqlBuilder.lastSpaceCharacter()) {
            sqlBuilder.appendSql(" ");
            sqlBuilder.appendBuilder(tmp);
        } else {
            sqlBuilder.appendBuilder(tmp);
        }
    }

    @Override
    public String toString() {
        return "pairs [" + this.hashCode() + "]";
    }
}
