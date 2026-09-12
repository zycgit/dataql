/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.logic;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * <bind> 标签
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-05-24
 */
public class BindDynamicSql implements DynamicSql {
    private final String name;      // 名字
    private final String valueExpr; // 值

    public BindDynamicSql(String name, String valueExpr) {
        this.name = name;
        this.valueExpr = valueExpr;
    }

    @Override
    public boolean isHaveInjection() {
        return false;
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) {
        if (StringUtils.isNotBlank(this.name)) {
            Object testExprResult = OgnlUtils.evalOgnl(this.valueExpr, data);
            data.putValue(this.name, testExprResult);
        }
    }
}
