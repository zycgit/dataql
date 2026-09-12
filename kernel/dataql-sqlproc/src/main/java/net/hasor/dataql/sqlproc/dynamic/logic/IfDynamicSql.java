/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.logic;
import java.sql.SQLException;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * <if> 标签
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-05-24
 */
public class IfDynamicSql extends ArrayDynamicSql {
    private final String testExpr;   // 判断表达式

    public IfDynamicSql(String testExpr) {
        this.testExpr = testExpr;
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        if (test(data)) {
            this.buildBody(data, context, sqlBuilder);
        }
    }

    public void buildBody(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        super.buildQuery(data, context, sqlBuilder);
    }

    protected boolean test(SqlArgSource data) {
        Object testExprResult = OgnlUtils.evalOgnl(this.testExpr, data);
        return Boolean.TRUE.equals(testExprResult);
    }
}
