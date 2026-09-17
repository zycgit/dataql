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
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;
import net.hasor.dataql.sqlproc.types.SqlArgSource;
import static net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils.evalOgnl;

/**
 * 如果参数不为空，则生成 'column = ?'。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class IfRule implements SqlRule {
    public static final SqlRule INSTANCE_IF = new IfRule();

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        return StringUtils.isBlank(activeExpr) || Boolean.TRUE.equals(evalOgnl(activeExpr, data));
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) throws SQLException {
        if (ruleValue != null) {
            DynamicParsed.getParsedSql(ruleValue).buildQuery(data, context, sqlBuilder);
        }
    }

    @Override
    public String toString() {
        return "if [" + this.hashCode() + "]";
    }
}
