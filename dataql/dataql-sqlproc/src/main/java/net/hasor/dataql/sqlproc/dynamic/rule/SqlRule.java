/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.sql.SQLException;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.types.SqlArgSource;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;

/**
 * 动态 SQL 中定义的规则。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public interface SqlRule {
    boolean test(SqlArgSource data, QueryContext context, String activeExpr);

    void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) throws SQLException;
}
