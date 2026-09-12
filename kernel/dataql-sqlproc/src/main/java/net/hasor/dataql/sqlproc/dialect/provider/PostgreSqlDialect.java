/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/**
 * PostgreSQL 对象名有大小写敏感不敏感的问题
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public class PostgreSqlDialect extends AbstractDialect {
    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        StringBuilder sqlBuilder = new StringBuilder(boundSql.getSqlString());
        List<Object> paramArrays = new ArrayList<>(Arrays.asList(boundSql.getArgs()));

        if (limit > 0) {
            sqlBuilder.append(" LIMIT ?");
            paramArrays.add(limit);
        }
        if (start > 0) {
            sqlBuilder.append(" OFFSET ?");
            paramArrays.add(start);
        }

        return new BoundSql.BoundSqlObj(sqlBuilder.toString(), paramArrays.toArray());
    }
}
