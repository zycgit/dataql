/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/** Pagination for Elastic JDBC commands. */
public class ElasticDialect extends AbstractDialect {
    @Override
    public BoundSql countSql(BoundSql boundSql) {
        return new BoundSql.BoundSqlObj("/*+overwrite_find_as_count*/" + boundSql.getSqlString(), boundSql.getArgs());
    }

    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        StringBuilder sqlBuilder = new StringBuilder("/*+");

        if (start <= 0) {
            sqlBuilder.append("overwrite_find_limit=" + limit);
        } else {
            sqlBuilder.append("overwrite_find_skip=" + start + ",overwrite_find_limit=" + limit);
        }

        sqlBuilder.append("*/");
        return new BoundSql.BoundSqlObj(sqlBuilder + boundSql.getSqlString(), boundSql.getArgs());
    }
}
