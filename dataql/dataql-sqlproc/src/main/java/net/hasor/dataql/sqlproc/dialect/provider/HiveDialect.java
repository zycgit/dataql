/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/** Pagination using Hive 2.0+ LIMIT offset, rows syntax. */
public class HiveDialect extends AbstractDialect {
    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        if (start < 0 || limit < 0) {
            throw new IllegalArgumentException("Hive pagination requires non-negative offset and limit");
        }

        // Hive requires integer constants in LIMIT; only business values remain JDBC parameters.
        StringBuilder sql = new StringBuilder(boundSql.getSqlString());
        sql.append(" LIMIT ");
        if (start > 0) {
            sql.append(start).append(", ");
        }
        sql.append(limit);
        return new BoundSql.BoundSqlObj(sql.toString(), boundSql.getArgs());
    }
}
