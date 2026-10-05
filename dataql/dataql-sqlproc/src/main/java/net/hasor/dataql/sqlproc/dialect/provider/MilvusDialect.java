/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/** Pagination for Milvus JDBC commands. */
public class MilvusDialect extends AbstractDialect {
    @Override
    public BoundSql countSql(BoundSql boundSql) {
        return new BoundSql.BoundSqlObj("/*+ overwrite_find_as_count=true */ " + boundSql.getSqlString(), boundSql.getArgs());
    }

    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        StringBuilder sb = new StringBuilder(boundSql.getSqlString());
        if (limit > 0) {
            sb.append(" LIMIT ").append(limit);
        }
        if (start > 0) {
            sb.append(" OFFSET ").append(start);
        }
        return new BoundSql.BoundSqlObj(sb.toString(), boundSql.getArgs());
    }
}
