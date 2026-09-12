/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.types.SqlArgSource;
import static net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils.evalOgnl;

/**
 * 动态参数规则，普通文本
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class TextRule implements SqlRule {
    public static final SqlRule INSTANCE = new TextRule(false);
    private final       boolean usingIf;

    public TextRule(boolean usingIf) {
        this.usingIf = usingIf;
    }

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        if (this.usingIf) {
            return StringUtils.isBlank(activeExpr) || Boolean.TRUE.equals(evalOgnl(activeExpr, data));
        } else {
            return true;
        }
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) {
        if (this.usingIf) {
            sqlBuilder.appendSql(ruleValue);
        } else {
            if (activeExpr != null) {
                sqlBuilder.appendSql(activeExpr);
                if (ruleValue != null) {
                    sqlBuilder.appendSql(",");
                }
            }

            if (ruleValue != null) {
                sqlBuilder.appendSql(ruleValue);
            }
        }
    }

    @Override
    public String toString() {
        return (this.usingIf ? "iftext [" : "text [") + this.hashCode() + "]";
    }
}
